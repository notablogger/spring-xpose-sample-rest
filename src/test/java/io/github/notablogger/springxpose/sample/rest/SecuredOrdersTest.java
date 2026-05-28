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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@DirtiesContext
class SecuredOrdersTest {

    @Autowired MockMvc mvc;

    @Test
    void unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void customer_canRead() throws Exception {
        mvc.perform(get("/api/orders").with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk());
    }

    @Test
    void customer_cannotWrite_returns403() throws Exception {
        mvc.perform(post("/api/orders")
                .with(httpBasic("customer", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reference":"ORD-X","totalAmount":10.0,"status":"NEW"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_createReadUpdateLifecycle() throws Exception {
        MvcResult result = mvc.perform(post("/api/orders")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"ORD-ADMIN","totalAmount":250.0,"status":"NEW"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("ORD-ADMIN"))
                .andReturn();
        long id = extractId(result.getResponse().getContentAsString());

        mvc.perform(get("/api/orders/" + id).with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk());

        mvc.perform(put("/api/orders/" + id)
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"ORD-ADMIN","totalAmount":250.0,"status":"SHIPPED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mvc.perform(put("/api/orders/" + id)
                        .with(httpBasic("customer", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"ORD-ADMIN","totalAmount":250.0,"status":"HACKED"}
                                """))
                .andExpect(status().isForbidden());

    }

    private long extractId(String json) {
        int idx = json.indexOf("\"id\":");
        int start = idx + 5;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Long.parseLong(json.substring(start, end).trim());
    }
}
