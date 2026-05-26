package io.github.notablogger.springxpose.sample.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class ValidationTest {

    @Autowired MockMvc mvc;

    @Test
    void category_blankName_returns400() throws Exception {
        mvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","description":"desc"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void category_missingName_returns400() throws Exception {
        mvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"description":"no name"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void product_blankName_returns400() throws Exception {
        mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","price":9.99}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void product_negativePrice_returns400() throws Exception {
        mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Bad","price":-1.0}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void category_notFound_returns404() throws Exception {
        mvc.perform(get("/api/categories/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void product_notFound_returns404() throws Exception {
        mvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }
}
