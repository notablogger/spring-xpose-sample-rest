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

import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
class PublicCrudTest {

    @Autowired MockMvc mvc;

    @Test
    void category_create_returns201() throws Exception {
        mvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Electronics","description":"Gadgets"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                // CustomCategoryMapper upper-cases names in responses (demo of custom mapping)
                .andExpect(jsonPath("$.name").value("ELECTRONICS"));
    }

    @Test
    void category_fullLifecycle() throws Exception {
        MvcResult result = mvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Books","description":"All books"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long id = extractId(result.getResponse().getContentAsString());

        mvc.perform(get("/api/categories/" + id))
                .andExpect(status().isOk())
                // CustomCategoryMapper upper-cases name in response
                .andExpect(jsonPath("$.name").value("BOOKS"));

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));

        mvc.perform(put("/api/categories/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Books & Magazines","description":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BOOKS & MAGAZINES"));

        mvc.perform(delete("/api/categories/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/categories/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void product_create_returns201() throws Exception {
        mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Widget","price":9.99,"description":"A small widget"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price").value(9.99));
    }

    @Test
    void product_fullLifecycle() throws Exception {
        MvcResult result = mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Gadget","price":49.99,"description":"desc"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long id = extractId(result.getResponse().getContentAsString());

        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(49.99));

        mvc.perform(put("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Gadget Pro","price":59.99,"description":"upgraded"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gadget Pro"));

        mvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound());
    }

    private long extractId(String json) {
        int idx = json.indexOf("\"id\":");
        int start = idx + 5;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Long.parseLong(json.substring(start, end).trim());
    }
}
