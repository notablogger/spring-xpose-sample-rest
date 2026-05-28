# spring-xpose-sample-rest

A runnable Spring Boot sample demonstrating **[spring-xpose](https://github.com/notablogger/spring-xpose)** — a library that generates full REST APIs (repository, DTO, request DTO, mapper, controller, security) from a single annotation at compile time (`@ExposeEntity` for JPA and `@ExposeDocument` for MongoDB).

---

## What this sample shows

Five real-world scenarios, each demonstrating a different spring-xpose configuration:

| Entity | Auth | Roles | Relation mode | `ignoredFields` | What it demonstrates |
|---|---|---|---|---|---|
| `Category` | None (public) | — | — | — | Zero-config public CRUD |
| `Product` | None (public) | — | `ALWAYS_OBJECT` | `description` | Relation as full object; field hidden from API |
| `Order` | HTTP Basic | read: `CUSTOMER`, `ADMIN` / write: `ADMIN` | `IDS_FOR_LIST_OBJECT_FOR_SINGLE` | — | Role-based read/write split |
| `Report` | OAuth2 Bearer | read: `VIEWER`, `ADMIN` / write: `ADMIN` | — | — | JWT-protected CRUD and role-based access |
| `Note` | HTTP Basic | read: `CUSTOMER`, `ADMIN` / write: `ADMIN` | — | — | MongoDB document CRUD via `@ExposeDocument` |

No controllers, repositories, DTOs, request DTOs, mappers, or security configs were written by hand. Everything is generated at compile time.

---

## Prerequisites

- Java 21+
- Docker (for real Postgres + Mongo via Compose)
- Gradle (wrapper included — no install needed)

---

## Step 1 — Clone

```bash
git clone https://github.com/notablogger/spring-xpose-sample-rest.git
cd spring-xpose-sample-rest
```

---

## Step 2 — Start databases (real services)

```bash
docker compose up -d postgres mongo
```

By default this starts:

- PostgreSQL at `localhost:5432` (`db: xpose`, `user: xpose`, `password: xpose`)
- MongoDB at `localhost:27017` (`db: xpose`)

PostgreSQL schema is auto-created by Hibernate (`ddl-auto=update`) and sample relational data is seeded from `db/sql/data.sql`. Mongo `notes` sample documents are initialized by `docker/mongo/init-notes.js`.

If you want a fully fresh seed state, recreate containers and volumes first:

```bash
docker compose down -v
docker compose up -d postgres mongo
```

---

## Step 3 — Run the app

```bash
./gradlew bootRun
```

### Local profile (easy OAuth testing)

If you want to test `/api/reports` locally without configuring a real JWKS provider, run with the `local` profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Then call secured report endpoints with any bearer token:

```bash
curl -i -H 'Authorization: Bearer local-test-token' http://localhost:8080/api/reports

curl -i \
  -H 'Authorization: Bearer local-test-token' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Local Report","content":"manual","score":1.0}' \
  http://localhost:8080/api/reports
```

If your databases are not on default ports/credentials, pass env vars before running:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/xpose'
export SPRING_DATASOURCE_USERNAME='xpose'
export SPRING_DATASOURCE_PASSWORD='xpose'
export SPRING_DATA_MONGODB_URI='mongodb://localhost:27017/xpose'
./gradlew bootRun --args='--spring.profiles.active=local'
```

On first run Gradle downloads dependencies from Maven Central. The app starts on port `8080`.

You will see in the logs:

```
=========================================================
  spring-xpose REST sample started
  Swagger UI  → http://localhost:8080/swagger-ui.html
  JDBC URL    → jdbc:postgresql://localhost:5432/xpose
  Mongo URI   → mongodb://localhost:27017/xpose
=========================================================
```

---

## Step 4 — Explore the API

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

**List notes from MongoDB as customer (Basic auth):**
```bash
curl -u customer:customer123 http://localhost:8080/api/notes
# → [{"id":"...","title":"Welcome Note","content":"...","author":"system"}, ...]
```

**Notes without auth — expect 401:**
```bash
curl -i http://localhost:8080/api/notes
# → 401 Unauthorized
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

**Reports without bearer token — expect 401:**
```bash
curl -i http://localhost:8080/api/reports
# → 401 Unauthorized
```

**Reports with bearer token (local profile) — expect 200:**
```bash
curl -i -H 'Authorization: Bearer local-test-token' http://localhost:8080/api/reports
# → 200 OK
```

**Invalid relation (example: `productId` does not exist) — expect sanitized 409:**
```bash
curl -i -u admin:admin123 \
  -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"reference":"ORD-BAD","totalAmount":10.0,"status":"NEW","productId":0}'
```

Example response body:
```json
{
  "type": "urn:springxpose:constraint-violation",
  "title": "Data integrity violation",
  "status": 409,
  "detail": "The request conflicts with existing data. Verify referenced IDs and unique values.",
  "errorCode": "CONSTRAINT_VIOLATION"
}
```

`spring-xpose` intentionally does not return raw SQL/constraint names in API responses.

In this sample, constraint violations are sanitized by a local override in
`src/main/java/io/github/notablogger/springxpose/sample/rest/config/SampleExceptionHandler.java`.
That lets the sample keep the server-side stack trace in logs while returning a safe API payload.

---

## Exception handling

`spring-xpose` ships with a global exception handler in
`spring-xpose` starter: `SpringXposeExceptionHandler`.

### Exceptions handled by the library

| Exception type | Status | Problem type | What it means |
|---|---:|---|---|
| `HttpMessageNotReadableException` | `400` | `urn:springxpose:malformed-body` | The JSON body is malformed or cannot be parsed |
| `MethodArgumentNotValidException` | `400` | `urn:springxpose:validation-error` | Bean validation failed (`@NotBlank`, `@Positive`, `@Size`, etc.) |
| `EntityNotFoundException` | `422` | `urn:springxpose:relation-not-found` | A relation ID in the request points to an entity that does not exist |
| `DataIntegrityViolationException` | `409` | `urn:springxpose:constraint-violation` | A database constraint failed (duplicate key, FK violation, NOT NULL, etc.) |
| `OptimisticLockingFailureException` | `409` | `urn:springxpose:optimistic-lock` | Two requests updated the same versioned row concurrently |

### Sample-specific override

The sample app adds its own `@RestControllerAdvice` in
`SampleExceptionHandler` to override the library behavior for
`DataIntegrityViolationException`.

Why the override exists:

- to guarantee no raw SQL or constraint names leak to API clients
- to return a stable `errorCode` (`CONSTRAINT_VIOLATION`)
- to keep detailed exception information in server logs only

The sample currently overrides:

| Exception type | Defined in | Behavior |
|---|---|---|
| `DataIntegrityViolationException` | `spring-xpose-sample-rest` | Returns a sanitized `409` payload with generic conflict details |

All other exception types continue to use the default handler from the `spring-xpose` starter.

---

## Step 5 — See the generated code

After `./gradlew build`, open:

```
build/generated/sources/annotationProcessor/java/main/
  io/github/notablogger/springxpose/sample/rest/entity/generated/
```

You will find **six** generated files per entity:

```
CategoryRepository.java          ← JpaRepository<Category, Long>
CategoryDto.java                 ← clean response shape (no JPA annotations)
CategoryRequestDto.java          ← request body shape for CREATE/UPDATE
CategoryMapper.java              ← MapStruct: toDto(), toDtoList(), toEntity(), updateEntity()
CategoryController.java          ← @RestController at /api/categories
CategorySecurityConfigurer.java  ← permitAll() SecurityFilterChain

ProductRepository.java
ProductDto.java                  ← no 'description' field (ignoredFields = {"description"})
ProductRequestDto.java
ProductMapper.java
ProductController.java
ProductSecurityConfigurer.java

OrderRepository.java
OrderDto.java                    ← productId: Long (IDS mode for list)
OrderRequestDto.java
OrderMapper.java                 ← @Mapping(source="product.id", target="productId")
OrderController.java             ← @RestController at /api/orders
OrderSecurityConfigurer.java     ← Basic auth, CUSTOMER/ADMIN roles

NoteRepository.java              ← MongoRepository<Note, String>
NoteDto.java
NoteRequestDto.java
NoteMapper.java
NoteController.java              ← @RestController at /api/notes
NoteSecurityConfigurer.java      ← Basic auth, CUSTOMER/ADMIN roles
```

These are real `.java` files — open them in your IDE, read them, set breakpoints.

---

## Step 6 — Understand `ignoredFields`

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

### HTTP Basic (for `/api/orders` and `/api/notes`)

| Username | Password | Role | Access |
|---|---|---|---|
| `customer` | `customer123` | `ROLE_CUSTOMER` | GET only |
| `admin` | `admin123` | `ROLE_ADMIN` | GET + POST + PUT (`DELETE` not exposed for `Order`) |

---

## Project structure

```
src/main/java/.../
  RestSampleApplication.java      ← @SpringBootApplication entry point
  config/
    SampleExceptionHandler.java   ← Sample override for sanitized constraint violation responses
    SecurityConfig.java           ← Fallback chain (Swagger UI + docs)
  entity/
    Category.java                 ← @ExposeEntity, public, full CRUD
    Product.java                  ← @ExposeEntity, public, ALWAYS_OBJECT, ignoredFields
    Order.java                    ← @ExposeEntity, Basic auth, role-split
    Report.java                   ← @ExposeEntity, OAuth2/JWT role-split
    Note.java                     ← @ExposeDocument, MongoDB + Basic auth role-split

src/main/resources/
  db/sql/
    data.sql                      ← Relational sample seed data (schema is Hibernate-managed)

docker/mongo/
  init-notes.js                   ← Mongo sample notes seed script
```

---

## Useful URLs

| URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI — try all endpoints interactively |
| http://localhost:8080/v3/api-docs | Raw OpenAPI JSON spec |

---

## Optional: run everything in Docker

```bash
docker compose up --build
```

This launches `postgres`, `mongo`, and `rest-sample` together.

---

## Related

- **[spring-xpose](https://github.com/notablogger/spring-xpose)** — the library this sample uses
- **[Maven Central](https://central.sonatype.com/search?q=io.github.notablogger)** — published artifacts

---

## Using `spring-xpose` in Production

The sample app demonstrates the library's capabilities. To deploy a real service:

1. **Authentication & Authorization**
   - Enable OAuth2 resource server with real JWKS provider (uncomment in `application.yml`)
   - Integrate with your identity provider (Keycloak, Auth0, etc.)
   - Replace in-memory users with database or external auth service

2. **Configuration Management**
   - Use environment-specific profiles (`prod`, `staging`, `dev`)
   - Set sensitive values (DB credentials, JWKS URIs) via environment variables or secrets manager
   - Disable SQL logging and auto-index creation in production

3. **Database Migration**
   - Use Flyway or Liquibase for schema versioning instead of `spring.sql.init.mode: always`
   - Test migrations in staging before production deployments

4. **Monitoring & Observability**
   - Enable Spring Boot Actuator endpoints (health, metrics)
   - Configure structured logging (JSON format)
   - Use distributed tracing for multi-service deployments

5. **Security Hardening**
   - Enable CSRF protection based on your SPA/client architecture
   - Set appropriate CORS policies
   - Use HTTPS only in production
   - Implement rate limiting and DDoS protection

---

## License

Apache 2.0
