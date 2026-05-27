package io.github.notablogger.springxpose.sample.rest;

import io.github.notablogger.springxpose.sample.rest.entity.Category;
import io.github.notablogger.springxpose.sample.rest.entity.Order;
import io.github.notablogger.springxpose.sample.rest.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("legacy-seed")
public class DataLoader implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate transactionTemplate;

    public DataLoader(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        transactionTemplate.executeWithoutResult(status -> loadData());
    }

    private void loadData() {
        // ── Categories ──
        Category electronics = new Category();
        electronics.setName("Electronics");
        electronics.setDescription("Gadgets and devices");
        em.persist(electronics);

        Category books = new Category();
        books.setName("Books");
        books.setDescription("Fiction and non-fiction");
        em.persist(books);

        // ── Products ──
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

        // ── Orders ──
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
              spring-xpose REST sample started (legacy-seed profile)
              Swagger UI  → http://localhost:8080/swagger-ui.html
              Relational DB seeded via legacy DataLoader
            =========================================================
            """);
    }
}
