package io.github.notablogger.springxpose.sample.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Query filters declared with {@code @ExposeField(filterOperations = ...)} on the
 * generated findAll endpoints, against the seed data in {@code db/sql/data.sql}:
 *
 * <pre>
 * Product      price    category      tags
 * Laptop Pro   1299.99  Electronics   sale, new
 * Clean Code     34.99  Books         bestseller
 * Mystery Box     9.99  —             —
 *
 * Order        status     product
 * ORD-0001     PENDING    Laptop Pro
 * ORD-0002     SHIPPED    Clean Code
 * ORD-0003     CANCELLED  Laptop Pro
 * </pre>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@DirtiesContext
class FilterTest {

    @Autowired MockMvc mvc;

    // ── Product: scalar fields ────────────────────────────────────────────────

    @Test
    void product_likeIsCaseInsensitiveContains() throws Exception {
        mvc.perform(get("/api/products").param("name_like", "LAPTOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro")));
    }

    @Test
    void product_priceRange() throws Exception {
        mvc.perform(get("/api/products").param("price_gte", "30").param("price_lte", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Clean Code")));
    }

    @Test
    void product_invalidValue_returns400KeyedByParameter() throws Exception {
        mvc.perform(get("/api/products").param("price_gte", "cheap"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price_gte").exists());
    }

    // ── Product: relation and collection filters ──────────────────────────────

    @Test
    void product_byCategoryId() throws Exception {
        long electronics = idOf("/api/products?name=Laptop Pro", "$[0].category.id");

        mvc.perform(get("/api/products").param("categoryId", String.valueOf(electronics)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro")));
    }

    @Test
    void product_withoutCategory() throws Exception {
        mvc.perform(get("/api/products").param("categoryId_is_null", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Mystery Box")));
    }

    @Test
    void product_tagsHasAnyHasNoneAndEmpty() throws Exception {
        mvc.perform(get("/api/products").param("tags_in", "sale,bestseller"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro", "Clean Code")));

        mvc.perform(get("/api/products").param("tags_not_in", "sale"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Clean Code", "Mystery Box")));

        mvc.perform(get("/api/products").param("tags_is_null", "true"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Mystery Box")));
    }

    // ── Product: hidden field ─────────────────────────────────────────────────

    @Test
    void product_hiddenDescription_isNotReturnedAndNotFilterable() throws Exception {
        mvc.perform(get("/api/products").param("name", "Clean Code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", not(hasKey("description"))));

        // no filter exists for a hidden field — the parameter is ignored, nothing is revealed
        mvc.perform(get("/api/products").param("description", "no such description"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro", "Clean Code", "Mystery Box")));
    }

    // ── Order (BASIC auth) ────────────────────────────────────────────────────

    @Test
    void order_statusNotInAndReferenceLike() throws Exception {
        mvc.perform(get("/api/orders").param("status_not_in", "CANCELLED")
                        .with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].reference", containsInAnyOrder("ORD-0001", "ORD-0002")));

        mvc.perform(get("/api/orders").param("status", "SHIPPED").param("reference_like", "ord-000")
                        .with(httpBasic("customer", "customer123")))
                .andExpect(jsonPath("$[*].reference", containsInAnyOrder("ORD-0002")));
    }

    @Test
    void order_byProductId() throws Exception {
        long laptop = idOf("/api/products?name=Laptop Pro", "$[0].id");

        mvc.perform(get("/api/orders").param("productId", String.valueOf(laptop))
                        .with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].reference", containsInAnyOrder("ORD-0001", "ORD-0003")));
    }

    // ── Category: to-many relation ────────────────────────────────────────────

    @Test
    void category_nameLikeAndByProductId() throws Exception {
        // CustomCategoryMapper upper-cases names in responses
        mvc.perform(get("/api/categories").param("name_like", "book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("BOOKS")));

        long laptop = idOf("/api/products?name=Laptop Pro", "$[0].id");
        mvc.perform(get("/api/categories").param("products_in", String.valueOf(laptop)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("ELECTRONICS")));
    }

    // ── Report (OAUTH2) ───────────────────────────────────────────────────────

    @Test
    void report_scoreGte() throws Exception {
        mvc.perform(get("/api/reports").param("score_gte", "4.5")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Spring Xpose Launch Report")));

        mvc.perform(get("/api/reports").param("score_gte", "5")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private long idOf(String url, String jsonPath) throws Exception {
        String body = mvc.perform(get(url)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, jsonPath)).longValue();
    }
}
