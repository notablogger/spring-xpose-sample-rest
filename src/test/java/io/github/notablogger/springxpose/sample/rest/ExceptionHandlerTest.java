package io.github.notablogger.springxpose.sample.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the global {@code SpringXposeExceptionHandler} — verifies that all error
 * conditions produce consistent RFC 9457 Problem Detail JSON responses.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
class ExceptionHandlerTest {

    @Autowired MockMvc mvc;

    // ── 400 Malformed JSON ───���────────────────────────────────────────────────

    @Test
    void malformedJson_returns400_withProblemDetail() throws Exception {
        mvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ this is not json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request body"))
                .andExpect(jsonPath("$.type").value("urn:springxpose:malformed-body"));
    }

    // ── 400 Validation ───────────────────────────────────────────────────────

    @Test
    void validationFailure_returns400_withFieldErrors() throws Exception {
        mvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","description":"desc"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.type").value("urn:springxpose:validation-error"))
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void multipleValidationErrors_allFieldsReported() throws Exception {
        mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","price":-5.0}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    // ── 404 Not Found ────────────────────────────────────────────────────────

    @Test
    void getByNonExistentId_returns404() throws Exception {
        mvc.perform(get("/api/categories/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNonExistentEntity_returns404() throws Exception {
        mvc.perform(put("/api/categories/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ghost"}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNonExistentEntity_returns404() throws Exception {
        mvc.perform(delete("/api/categories/999999"))
                .andExpect(status().isNotFound());
    }

    // ── PUT load-then-merge (no blind overwrite) ──────────────────────────────

    @Test
    void update_preservesFieldsNotInRequest() throws Exception {
        // Create with description
        MvcResult created = mvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Tech","description":"Technology category"}
                        """))
                .andExpect(status().isCreated())
                .andReturn();
        long id = extractId(created.getResponse().getContentAsString());

        // Update only the name — description must NOT be wiped
        mvc.perform(put("/api/categories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Technology"}
                        """))
                .andExpect(status().isOk())
                // CustomCategoryMapper uppercases name
                .andExpect(jsonPath("$.name").value("TECHNOLOGY"))
                // description was not sent in PUT body but must be preserved (load-then-merge)
                .andExpect(jsonPath("$.description").value("Technology category"));
    }

    private long extractId(String json) {
        int idx = json.indexOf("\"id\":");
        int start = idx + 5;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Long.parseLong(json.substring(start, end).trim());
    }
}

