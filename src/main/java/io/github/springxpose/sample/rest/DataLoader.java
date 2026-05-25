package io.github.springxpose.sample.rest;

import io.github.springxpose.sample.rest.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the H2 database with representative data for every entity variant
 * so the Swagger UI is immediately usable after startup.
 */
@Configuration
public class DataLoader {

    @PersistenceContext
    private EntityManager em;

    @Bean
    @Transactional
    @Profile("!test")
    CommandLineRunner seed() {
        return args -> loadData();
    }

    @Transactional
    public void loadData() {
        // ── Categories (no-auth, full CRUD, relation not serialised outward) ──
        Category electronics = new Category();
        electronics.setName("Electronics");
        electronics.setDescription("Gadgets and devices");
        em.persist(electronics);

        Category books = new Category();
        books.setName("Books");
        books.setDescription("Fiction and non-fiction");
        em.persist(books);

        // ── Products (no-auth, full CRUD, relation ALWAYS_OBJECT) ──
        Product laptop = new Product();
        laptop.setName("Laptop Pro");
        laptop.setPrice(1299.99);
        laptop.setDescription("High-performance laptop");
        laptop.setCategory(electronics);
        em.persist(laptop);

        Product novel = new Product();
        novel.setName("Clean Code");
        novel.setPrice(34.99);
        novel.setDescription("A handbook of agile software craftsmanship");
        novel.setCategory(books);
        em.persist(novel);

        // ── Authors (no-auth, full CRUD) ──
        Author alice = new Author();
        alice.setName("Alice Smith");
        alice.setBio("Writes about technology and open source.");
        em.persist(alice);

        Author bob = new Author();
        bob.setName("Bob Jones");
        bob.setBio("Specialises in distributed systems.");
        em.persist(bob);

        // ── Articles (read-only, no-auth, relation ALWAYS_IDS) ──
        Article a1 = new Article();
        a1.setTitle("Introduction to spring-xpose");
        a1.setContent("spring-xpose generates REST APIs from JPA entities automatically.");
        a1.setAuthor(alice);
        em.persist(a1);

        Article a2 = new Article();
        a2.setTitle("Distributed Systems 101");
        a2.setContent("An overview of CAP theorem and eventual consistency.");
        a2.setAuthor(bob);
        em.persist(a2);

        // ── Orders (Basic auth, CUSTOMER/ADMIN roles, relation default mode) ──
        Order o1 = new Order();
        o1.setReference("ORD-0001");
        o1.setTotalAmount(1299.99);
        o1.setStatus("PENDING");
        o1.setProduct(laptop);
        em.persist(o1);

        Order o2 = new Order();
        o2.setReference("ORD-0002");
        o2.setTotalAmount(34.99);
        o2.setStatus("SHIPPED");
        o2.setProduct(novel);
        em.persist(o2);

        // ── Reports (OAuth2 / JWT, ANALYST/ADMIN roles, no relations) ──
        Report r1 = new Report();
        r1.setTitle("Q1 Sales Report");
        r1.setSummary("Revenue up 12% vs prior quarter.");
        r1.setGeneratedAt("2026-04-01");
        em.persist(r1);

        // ── Tags (read-only, no-auth, no relations) ──
        for (String[] pair : new String[][]{
            {"java", "#E76F51"}, {"spring", "#2A9D8F"}, {"rest", "#E9C46A"},
            {"security", "#264653"}, {"openapi", "#F4A261"}
        }) {
            Tag tag = new Tag();
            tag.setName(pair[0]);
            tag.setColour(pair[1]);
            em.persist(tag);
        }

        System.out.println("""

            =========================================================
              spring-xpose REST sample started
              Swagger UI  → http://localhost:8080/swagger-ui.html
              OpenAPI JSON→ http://localhost:8080/v3/api-docs
              H2 console  → http://localhost:8080/h2-console
              JDBC URL    → jdbc:h2:mem:restdb

              Basic-auth users (for /api/orders):
                customer / customer123  (ROLE_CUSTOMER — read only)
                admin    / admin123     (ROLE_ADMIN    — full CRUD)

              OAuth2 endpoints (for /api/reports):
                Send Bearer JWT with scope ANALYST or ADMIN
            =========================================================
            """);
    }
}
