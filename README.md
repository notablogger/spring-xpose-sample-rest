# spring-xpose-sample-rest

A runnable Spring Boot sample demonstrating **[spring-xpose](https://github.com/notablogger/spring-xpose)** — a library that generates full REST APIs (repository, DTO, mapper, controller, security) from a single `@ExposeEntity` annotation at compile time.

---

## What this sample shows

Three real-world entity scenarios, each demonstrating a different spring-xpose configuration:

| Entity | Auth | Roles | Relation mode | `ignoredFields` | What it demonstrates |
|---|---|---|---|---|---|
| `Category` | None (public) | — | — | — | Zero-config public CRUD |
| `Product` | None (public) | — | `ALWAYS_OBJECT` | `description` | Relation as full object; field hidden from API |
| `Order` | HTTP Basic | read: `CUSTOMER`, `ADMIN` / write: `ADMIN` | `IDS_FOR_LIST_OBJECT_FOR_SINGLE` | — | Role-based read/write split |

No controllers, repositories, DTOs, mappers, or security configs were written by hand. Everything is generated at compile time.

---

## Prerequisites

- Java 21+
- Gradle (wrapper included — no install needed)

---

## Step 1 — Clone

```bash
git clone https://github.com/notablogger/spring-xpose-sample-rest.git
cd spring-xpose-sample-rest
```

---

## Step 2 — Run

```bash
./gradlew bootRun
```

On first run Gradle downloads dependencies from Maven Central. The app starts on port `8080` and seeds the H2 database with demo data.

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
- **Order** endpoints — 🔒 lock icon, click **Authorize** → enter credentials below

### Option B — curl

**List categories (public):**
```bash
curl http://localhost:8080/api/categories
# → [{"id":1,"name":"Electronics","description":"Gadgets and devices"}, ...]
```

**List products — notice `description` is absent (ignoredFields):**
```bash
curl http://localhost:8080/api/products
# → [{"id":1,"name":"Laptop Pro","price":1299.99,"category":{"id":1,"name":"Electronics",...}}, ...]
```

**Orders without auth — expect 401:**
```bash
curl -i http://localhost:8080/api/orders
# → 401 Unauthorized
```

**List orders as customer:**
```bash
curl -u customer:customer123 http://localhost:8080/api/orders
# → [{"id":1,"reference":"ORD-0001","totalAmount":1299.99,"status":"PENDING","productId":1}, ...]
```

**Create an order as admin:**
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"reference":"ORD-TEST","totalAmount":49.99,"status":"NEW"}'
# → 201 Created
```

**Create an order as customer — expect 403:**
```bash
curl -i -u customer:customer123 \
  -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"reference":"ORD-HACK","totalAmount":0,"status":"FREE"}'
# → 403 Forbidden
```

---

## Step 4 — See the generated code

After `./gradlew build`, open:

```
build/generated/sources/annotationProcessor/java/main/
  io/github/notablogger/springxpose/sample/rest/entity/generated/
```

You will find **five** generated files per entity:

```
CategoryRepository.java          ← JpaRepository<Category, Long>
CategoryDto.java                 ← clean response shape (no JPA annotations)
CategoryMapper.java              ← MapStruct: toDto(), toDtoList()
CategoryController.java          ← @RestController at /api/categories
CategorySecurityConfigurer.java  ← permitAll() SecurityFilterChain

ProductRepository.java
ProductDto.java                  ← no 'description' field (ignoredFields = {"description"})
ProductMapper.java
ProductController.java
ProductSecurityConfigurer.java

OrderRepository.java
OrderDto.java                    ← productId: Long (IDS mode for list)
OrderMapper.java                 ← @Mapping(source="product.id", target="productId")
OrderController.java             ← @RestController at /api/orders
OrderSecurityConfigurer.java     ← Basic auth, CUSTOMER/ADMIN roles
```

These are real `.java` files — open them in your IDE, read them, set breakpoints.

---

## Step 5 — Understand `ignoredFields`

Open `Product.java`:

```java
@Entity
@ExposeEntity(
    path         = "products",
    relationMode = RelationMode.ALWAYS_OBJECT,
    ignoredFields = {"description"}    // ← stored in DB, never returned in the API
)
public class Product {
    @Id private Long id;
    private String name;
    private Double price;
    private String description;        // ← persisted, but absent from ProductDto
    @ManyToOne Category category;
}
```

The generated `ProductDto` contains `id`, `name`, `price`, and `category` — but no `description`. The database column still exists; the field is just hidden from API consumers.

---

## Credentials

### HTTP Basic (for `/api/orders`)

| Username | Password | Role | Access |
|---|---|---|---|
| `customer` | `customer123` | `ROLE_CUSTOMER` | GET only |
| `admin` | `admin123` | `ROLE_ADMIN` | Full CRUD |

---

## Project structure

```
src/main/java/.../
  RestSampleApplication.java      ← @SpringBootApplication entry point
  DataLoader.java                 ← Seeds H2 with demo data on startup
  config/
    SecurityConfig.java           ← Fallback chain (Swagger UI, H2 console)
  entity/
    Category.java                 ← @ExposeEntity, public, full CRUD
    Product.java                  ← @ExposeEntity, public, ALWAYS_OBJECT, ignoredFields
    Order.java                    ← @ExposeEntity, Basic auth, role-split
```

---

## Useful URLs

| URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI — try all endpoints interactively |
| http://localhost:8080/v3/api-docs | Raw OpenAPI JSON spec |
| http://localhost:8080/h2-console | H2 console — JDBC URL: `jdbc:h2:mem:restdb` |

---

## Related

- **[spring-xpose](https://github.com/notablogger/spring-xpose)** — the library this sample uses
- **[Maven Central](https://central.sonatype.com/search?q=io.github.notablogger)** — published artifacts

---

## License

Apache 2.0

