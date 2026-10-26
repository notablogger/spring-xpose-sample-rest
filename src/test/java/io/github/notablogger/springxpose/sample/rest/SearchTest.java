package io.github.notablogger.springxpose.sample.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@code POST /api/<path>/search} endpoints generated for {@code Operation.SEARCH}, with the filters
 * declared by {@code @ExposeField(filterOperations = ...)}, against the seed data in {@code db/sql/data.sql}:
 *
 * <pre>
 * Product      price    category      tags
 * Laptop Pro   1299.99  Electronics   sale, new
 * Clean Code     34.99  Books         bestseller
 * Mystery Box     9.99  —             —
 *
 * Order        status     amount    product      (Order is pageable — responses are pages)
 * ORD-0001     PENDING    1299.99   Laptop Pro
 * ORD-0002     SHIPPED      34.99   Clean Code
 * ORD-0003     CANCELLED  1299.99   Laptop Pro
 * </pre>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@DirtiesContext
class SearchTest {

    @Autowired MockMvc mvc;

    // ── Product: scalar fields ────────────────────────────────────────────────

    @Test
    void product_likeIsCaseInsensitiveContains() throws Exception {
        mvc.perform(search("/api/products", "{\"name_like\": \"LAPTOP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro")));
    }

    @Test
    void product_priceRange() throws Exception {
        mvc.perform(search("/api/products", "{\"price_gte\": 30, \"price_lte\": 100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Clean Code")));
    }

    @Test
    void product_emptyOrMissingBody_returnsEverything() throws Exception {
        mvc.perform(search("/api/products", "{}"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro", "Clean Code", "Mystery Box")));

        mvc.perform(post("/api/products/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro", "Clean Code", "Mystery Box")));
    }

    @Test
    void product_invalidValue_returns400KeyedByJsonKey() throws Exception {
        mvc.perform(search("/api/products", "{\"price_gte\": \"cheap\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price_gte").exists());
    }

    @Test
    void product_unknownKey_returns400WithTheAllowedKeys() throws Exception {
        // a typo, and an operation price does not declare (it has GTE and LTE only)
        for (String key : new String[] {"pirce_gte", "price_gt"}) {
            mvc.perform(search("/api/products", "{\"" + key + "\": 10}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors." + key, containsString("Unknown search key '" + key + "'")))
                    .andExpect(jsonPath("$.errors." + key, containsString("allowed keys: name, name_like, price_gte")));
        }
    }

    @Test
    void product_findAllIsUnfiltered() throws Exception {
        // filtering lives on POST /search only — GET ignores filter-like query parameters
        mvc.perform(get("/api/products").param("name_like", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro", "Clean Code", "Mystery Box")));
    }

    // ── Product: relation and collection filters ──────────────────────────────

    @Test
    void product_byCategoryId() throws Exception {
        long electronics = idOf(search("/api/products", "{\"name\": \"Laptop Pro\"}"), "$[0].category.id");

        mvc.perform(search("/api/products", "{\"categoryId\": " + electronics + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro")));
    }

    @Test
    void product_withoutCategory() throws Exception {
        mvc.perform(search("/api/products", "{\"categoryId_is_null\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Mystery Box")));
    }

    @Test
    void product_tagsHasAnyHasNoneAndEmpty() throws Exception {
        mvc.perform(search("/api/products", "{\"tags_in\": [\"sale\", \"bestseller\"]}"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Laptop Pro", "Clean Code")));

        mvc.perform(search("/api/products", "{\"tags_not_in\": [\"sale\"]}"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Clean Code", "Mystery Box")));

        mvc.perform(search("/api/products", "{\"tags_is_null\": true}"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Mystery Box")));
    }

    // ── Product: hidden field ─────────────────────────────────────────────────

    @Test
    void product_hiddenDescription_isNotReturnedAndNotSearchable() throws Exception {
        mvc.perform(search("/api/products", "{\"name\": \"Clean Code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", not(hasKey("description"))));

        // a hidden field is not a search key — rejected like any unknown key, nothing is revealed
        mvc.perform(search("/api/products", "{\"description\": \"no such description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description", containsString("Unknown search key 'description'")));
    }

    // ── Order (BASIC auth, pageable = true → responses are pages) ─────────────

    @Test
    void order_customerCanSearch_butNotCreate() throws Exception {
        // POST /search is a read: it gets the read roles, unlike POST /api/orders (create)
        mvc.perform(search("/api/orders", "{}").with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk());
        mvc.perform(search("/api/orders", "{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void order_statusNotInAndReferenceLike() throws Exception {
        mvc.perform(search("/api/orders", "{\"status_not_in\": [\"CANCELLED\"]}")
                        .with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].reference", containsInAnyOrder("ORD-0001", "ORD-0002")));

        mvc.perform(search("/api/orders", "{\"status\": \"SHIPPED\", \"reference_like\": \"ord-000\"}")
                        .with(httpBasic("customer", "customer123")))
                .andExpect(jsonPath("$.content[*].reference", containsInAnyOrder("ORD-0002")));
    }

    @Test
    void order_byProductId() throws Exception {
        long laptop = idOf(search("/api/products", "{\"name\": \"Laptop Pro\"}"), "$[0].id");

        mvc.perform(search("/api/orders", "{\"productId\": " + laptop + "}")
                        .with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].reference", containsInAnyOrder("ORD-0001", "ORD-0003")));
    }

    @Test
    void order_filtersCombineWithPagingAndSorting() throws Exception {
        // 2 orders match; page size 1, highest amount first → ORD-0001 (1299.99) on page 0, ORD-0002 on page 1
        mvc.perform(search("/api/orders", "{\"status_not_in\": [\"CANCELLED\"]}")
                        .param("page", "0").param("size", "1").param("sort", "totalAmount,desc")
                        .with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].reference", containsInAnyOrder("ORD-0001")))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(search("/api/orders", "{\"status_not_in\": [\"CANCELLED\"]}")
                        .param("page", "1").param("size", "1").param("sort", "totalAmount,desc")
                        .with(httpBasic("customer", "customer123")))
                .andExpect(jsonPath("$.content[*].reference", containsInAnyOrder("ORD-0002")));
    }

    // ── Category: plain CRUD, no SEARCH ───────────────────────────────────────

    @Test
    void category_exposesNoSearch() throws Exception {
        mvc.perform(search("/api/categories", "{}"))
                .andExpect(status().is4xxClientError());
    }

    // ── Report (OAUTH2) ───────────────────────────────────────────────────────

    @Test
    void report_scoreGte() throws Exception {
        mvc.perform(search("/api/reports", "{\"score_gte\": 4.5}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Spring Xpose Launch Report")));

        mvc.perform(search("/api/reports", "{\"score_gte\": 5}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static MockHttpServletRequestBuilder search(String path, String json) {
        return post(path + "/search").contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private long idOf(MockHttpServletRequestBuilder request, String jsonPath) throws Exception {
        String body = mvc.perform(request).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, jsonPath)).longValue();
    }
}
