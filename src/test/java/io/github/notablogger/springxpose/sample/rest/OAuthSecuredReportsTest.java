package io.github.notablogger.springxpose.sample.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for /api/reports — an endpoint secured with AuthType.OAUTH2.
 *
 * Uses spring-security-test's jwt() post-processor which injects a
 * pre-built JwtAuthenticationToken directly into the security context.
 * No real authorization server or JWKS endpoint is needed.
 *
 * Role mapping: Spring Security prefixes roles with ROLE_ internally,
 * so hasAnyRole("VIEWER") matches the authority "ROLE_VIEWER".
 * The jwt() builder accepts authorities as plain strings, so we pass
 * "ROLE_VIEWER" / "ROLE_ADMIN" to match what the generated SecurityConfigurer expects.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@DirtiesContext
class OAuthSecuredReportsTest {

    @Autowired MockMvc mvc;

    // ── 401 — no token ───────��───────────────────────────────────────────────

    @Test
    void noToken_returns401() throws Exception {
        mvc.perform(get("/api/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noToken_post_returns401() throws Exception {
        mvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Report A","content":"body","score":9.5}
                        """))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 — wrong role ─────────────────────────────────────────────────────

    @Test
    void unknownRole_get_returns403() throws Exception {
        mvc.perform(get("/api/reports")
                .with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_post_returns403() throws Exception {
        mvc.perform(post("/api/reports")
                .with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VIEWER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Sneaky","content":"body","score":1.0}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_put_returns403() throws Exception {
        mvc.perform(put("/api/reports/1")
                .with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VIEWER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Hacked","content":"body","score":1.0}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_delete_returns403() throws Exception {
        mvc.perform(delete("/api/reports/1")
                .with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isForbidden());
    }

    // ── VIEWER — read-only access ────────────────────────────────────────────

    @Test
    void viewerRole_getAll_returns200() throws Exception {
        mvc.perform(get("/api/reports")
                .with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk());
    }

    @Test
    void viewerRole_getById_notFound_returns404() throws Exception {
        mvc.perform(get("/api/reports/99999")
                .with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isNotFound());
    }

    // ── ADMIN — full CRUD lifecycle ──────────────────────────────────────────

    @Test
    void admin_fullLifecycle() throws Exception {
        var adminJwt = jwt().authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
        var viewerJwt = jwt().authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VIEWER"));

        // CREATE
        MvcResult created = mvc.perform(post("/api/reports")
                        .with(adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Q1 Report","content":"Q1 results","score":8.5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Q1 Report"))
                .andExpect(jsonPath("$.score").value(8.5))
                .andReturn();
        long id = extractId(created.getResponse().getContentAsString());

        // VIEWER can READ the created report
        mvc.perform(get("/api/reports/" + id).with(viewerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Q1 Report"));

        // UPDATE
        mvc.perform(put("/api/reports/" + id)
                        .with(adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Q1 Report (Final)","content":"Q1 results","score":9.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Q1 Report (Final)"))
                .andExpect(jsonPath("$.score").value(9.0));

        // VIEWER cannot update
        mvc.perform(put("/api/reports/" + id)
                        .with(viewerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hacked","content":"x","score":0.0}
                                """))
                .andExpect(status().isForbidden());

        // DELETE
        mvc.perform(delete("/api/reports/" + id).with(adminJwt))
                .andExpect(status().isNoContent());

        // Gone after delete — even viewer gets 404
        mvc.perform(get("/api/reports/" + id).with(viewerJwt))
                .andExpect(status().isNotFound());
    }

    // ── Validation still applies even with valid token ───────────────────────

    @Test
    void admin_blankTitle_returns400() throws Exception {
        mvc.perform(post("/api/reports")
                .with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"","content":"body","score":1.0}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:springxpose:validation-error"))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void admin_negativeScore_returns400() throws Exception {
        mvc.perform(post("/api/reports")
                .with(jwt().authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Bad","content":"body","score":-1.0}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.score").exists());
    }

    private long extractId(String json) {
        int idx = json.indexOf("\"id\":");
        int start = idx + 5;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Long.parseLong(json.substring(start, end).trim());
    }
}

