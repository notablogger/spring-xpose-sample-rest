# spring-xpose-sample-rest

A runnable Spring Boot sample demonstrating **[spring-xpose](https://github.com/notablogger/spring-xpose)** — a library that generates full REST APIs from a single `@ExposeEntity` annotation at compile time.

---

## What this sample shows

Three real-world entity scenarios, each demonstrating a different spring-xpose configuration:

| Entity | Auth | Roles | Relation mode | What it demonstrates |
|---|---|---|---|---|
| `Category` | None (public) | — | — | Zero-config public CRUD |
| `Product` | None (public) | — | `ALWAYS_OBJECT` | Relation serialised as full nested object |
| `Order` | HTTP Basic | read: `CUSTOMER`, `ADMIN` / write: `ADMIN` | `IDS_FOR_LIST_OBJECT_FOR_SINGLE` | Role-based read/write split |

No controllers, repositories, or security configs were written by hand. Everything is generated at compile time by spring-xpose.

---

## Prerequisites

- Java 21+
- Gradle (wrapper included — no install needed)

---

## Step 1 — Clone the repo

```bash
git clone https://github.com/notablogger/spring-xpose-sample-rest.git
cd spring-xpose-sample-rest
```

---

## Step 2 — Run the app

```bash
./gradlew bootRun
```

On first run Gradle will download dependencies from Maven Central. The app starts on port `8080`.

You will see in the logs:

```
=========================================================
  spring-xpose REST sample started
  Swagger UI  → http://localhost:8080/swagger-ui.html
  H2 console  → http://localhost:8080/h2-console
  JDBC URL    → jdbc:h2:mem:restdb
=========================================================
```

---

## Step 3 — Explore the API

### Option A — Swagger UI

Open **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

- **Category** and **Product** endpoints — no lock, fully public
- **Order** endpoints — 🔒 lock icon, click **Authorize** and enter credentials below

### Option B — curl

**List all categories (public):**
```bash
curl http://localhost:8080/api/categories
```

**List all products with nested category (public):**
```bash
curl http://localhost:8080/api/products
```

**Try orders without auth (expect 401):**
```bash
curl -i http://localhost:8080/api/orders
```

**List orders as a customer:**
```bash
curl -u customer:customer123 http://localhost:8080/api/orders
```

**Create an order as admin:**
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"reference":"ORD-TEST","totalAmount":49.99,"status":"NEW"}'
```

**Try to create an order as customer (expect 403):**
```bash
curl -i -u customer:customer123 \
  -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"reference":"ORD-HACK","totalAmount":0,"status":"FREE"}'
```

---

## Step 4 — See the generated code

After running `./gradlew build`, open:

```
build/generated/sources/annotationProcessor/java/main/
```

You will find — for each entity — three generated files:

```
io/github/notablogger/springxpose/sample/rest/entity/generated/
  CategoryRepository.java          ← JpaRepository<Category, Long>
  CategoryController.java          ← @RestController at /api/categories
  CategorySecurityConfigurer.java  ← permitAll() SecurityFilterChain

  ProductRepository.java
  ProductController.java
  ProductSecurityConfigurer.java

  OrderRepository.java
  OrderController.java             ← @RestController at /api/orders
  OrderSecurityConfigurer.java     ← Basic auth, CUSTOMER/ADMIN roles
```

These are real `.java` files — open them in your IDE, read them, set breakpoints.

---

## Credentials

### HTTP Basic (for `/api/orders`)

| Username | Password | Role | Access |
|---|---|---|---|
| `customer` | `customer123` | `ROLE_CUSTOMER` | GET only |
| `admin` | `admin123` | `ROLE_ADMIN` | Full CRUD |

---

## How it works

The `build.gradle` adds spring-xpose in three lines:

```groovy
implementation    'io.github.notablogger:spring-xpose-starter:0.1.3'
annotationProcessor 'io.github.notablogger:spring-xpose-processor:0.1.3'
compileOnly       'io.github.notablogger:spring-xpose-annotations:0.1.3'
```

Each entity is annotated with `@ExposeEntity`. For example, `Order.java`:

```java
@Entity
@Table(name = "orders")
@ExposeEntity(
    path       = "orders",
    expose     = {Operation.FIND_ALL, Operation.FIND_BY_ID,
                  Operation.CREATE,   Operation.UPDATE, Operation.DELETE},
    authType   = AuthType.BASIC,
    readRoles  = {"CUSTOMER", "ADMIN"},
    writeRoles = {"ADMIN"}
)
public class Order {
    @Id @GeneratedValue private Long id;
    private String reference;
    private Double totalAmount;
    private String status;
    @ManyToOne Product product;
    // getters / setters
}
```

That single annotation is everything spring-xpose needs to generate a fully secured, documented REST controller at build time.

---

## Useful URLs

| URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI — try all endpoints interactively |
| http://localhost:8080/v3/api-docs | Raw OpenAPI JSON spec |
| http://localhost:8080/h2-console | H2 web console — JDBC URL: `jdbc:h2:mem:restdb` |

---

## Project structure

```
src/main/java/.../
  RestSampleApplication.java     ← @SpringBootApplication entry point
  DataLoader.java                ← Seeds H2 with demo data on startup
  config/
    SecurityConfig.java          ← Fallback security chain (Swagger, H2 console)
  entity/
    Category.java                ← @ExposeEntity, public, full CRUD
    Product.java                 ← @ExposeEntity, public, ALWAYS_OBJECT relation
    Order.java                   ← @ExposeEntity, Basic auth, role-split
```

---

## Related

- **[spring-xpose](https://github.com/notablogger/spring-xpose)** — the library this sample uses
- **[Maven Central](https://central.sonatype.com/search?q=io.github.notablogger)** — published artifacts
- **GraphQL sample** — coming soon

---

## License

Apache 2.0

