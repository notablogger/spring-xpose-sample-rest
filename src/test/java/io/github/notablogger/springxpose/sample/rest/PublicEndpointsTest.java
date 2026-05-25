package io.github.notablogger.springxpose.sample.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicEndpointsTest {

    @Autowired MockMvc mvc;

    @Test
    void categories_findAll_returns200() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    void products_findAll_returns200() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void categories_findById_notFound_returns404() throws Exception {
        mvc.perform(get("/api/categories/99999")).andExpect(status().isNotFound());
    }

    @Test
    void products_findById_notFound_returns404() throws Exception {
        mvc.perform(get("/api/products/99999")).andExpect(status().isNotFound());
    }
}

