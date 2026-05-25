package com.notablogger.springxpose.sample.rest;

import com.notablogger.springxpose.sample.rest.entity.Category;
import com.notablogger.springxpose.sample.rest.entity.Order;
import com.notablogger.springxpose.sample.rest.entity.Product;
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

        System.out.println("""

            =========================================================
              spring-xpose REST sample started
              Swagger UI  → http://localhost:8080/swagger-ui.html
              H2 console  → http://localhost:8080/h2-console
              JDBC URL    → jdbc:h2:mem:restdb
            =========================================================
            """);
    }
}

