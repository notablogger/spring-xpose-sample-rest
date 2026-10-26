# spring-xpose-sample-rest

A runnable Spring Boot sample demonstrating **[spring-xpose](https://github.com/notablogger/spring-xpose)** — a library that generates full REST APIs (repository, DTO, request DTO, mapper, controller, security) from a single annotation at compile time (`@ExposeEntity` for JPA and `@ExposeDocument` for MongoDB).

---

## What this sample shows

Five real-world scenarios, each demonstrating a different spring-xpose configuration:

| Entity | Auth | Roles | Relation mode | Hidden fields | Search keys (`POST …/search`) | What it demonstrates |
|---|---|---|---|---|---|---|
| `Category` | None (public) | — | — | — | — (no `SEARCH`) | Zero-config public CRUD |
| `Product` | None (public) | — | `ALWAYS_OBJECT` | `description` | `name`, `name_like`, `price_gte` / `_lte`, `categoryId`, `categoryId_in` / `_is_null`, `tags_in` / `_not_in` / `_is_null` | Relation as full object; hidden field; scalar, relation and collection filters |
| `Order` | HTTP Basic | read: `CUSTOMER`, `ADMIN` / write: `ADMIN` | `IDS_FOR_LIST_OBJECT_FOR_SINGLE` | — | `status`, `status_in` / `_not_in`, `reference_like`, `totalAmount_gte` / `_lte`, `productId` | Role-based read/write split (search uses the read roles); `pageable = true` (search + `page` / `size` / `sort`) |
| `Report` | OAuth2 Bearer | read: `VIEWER`, `ADMIN` / write: `ADMIN` | — | — | `title_like`, `score_gte` | JWT-protected CRUD and role-based access |
| `Note` | HTTP Basic | read: `CUSTOMER`, `ADMIN` / write: `ADMIN` | — | — | `title_like`, `author`, `author_in` | MongoDB document CRUD via `@ExposeDocument` |

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

**List products — notice `description` is absent (`@ExposeField(hidden = true)`):**
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
# → {"content":[{"id":1,"reference":"ORD-0001","totalAmount":1299.99,"status":"PENDING","productId":1}, ...],
#    "totalElements":3,"totalPages":1,"number":0,"size":20, ...}
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
ProductDto.java                  ← no 'description' field (@ExposeField(hidden = true))
ProductSearchRequest.java        ← JSON body of POST /api/products/search: name, name_like, price_gte, categoryId, tags_in, …
ProductSpec.java                 ← turns the search body into a JPA Specification
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

## Step 6 — Hide a field with `@ExposeField(hidden = true)`

Open `Product.java`:

```java
@ExposeField(hidden = true)        // ← stored in DB, never returned or accepted by the API
private String description;
```

The generated `ProductDto` and `ProductRequestDto` contain no `description`. The database column still exists; the field is just hidden from API consumers. A hidden field can't have filters either — that would let clients guess its value — so searching for `{"description": "…"}` is rejected with `400` like any unknown key.

> Upgrading from 3.x? `ignoredFields = {"description"}` on `@ExposeEntity` was replaced by `@ExposeField(hidden = true)` on the field.

---

## Step 7 — Search with `Operation.SEARCH` + `@ExposeField(filterOperations = …)`

Entities that list `Operation.SEARCH` in `expose` get **`POST /api/<path>/search`**. Its JSON body takes the filters declared on the entity's fields; every key is optional and keys combine with AND. `Product`, `Order`, `Report` and `Note` expose it; `Category` doesn't, so it stays plain CRUD.

```java
// Product.java
@ExposeEntity(path = "products",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID, Operation.CREATE, Operation.UPDATE, Operation.DELETE,
              Operation.SEARCH}, ...)

@ExposeField(filterOperations = {EQ, LIKE})                  // {"name": "Clean Code"}  {"name_like": "laptop"}
private String name;

@ExposeField(filterOperations = {GTE, LTE})                  // {"price_gte": 50, "price_lte": 500}
private Double price;

@ManyToOne
@ExposeField(filterOperations = {EQ, IN, IS_NULL})           // {"categoryId": 1}  {"categoryId_in": [1, 2]}  {"categoryId_is_null": true}
private Category category;

@ElementCollection
@ExposeField(filterOperations = {IN, NOT_IN, IS_NULL})       // {"tags_in": ["sale", "new"]}  {"tags_not_in": ["sale"]}  {"tags_is_null": true}
private List<String> tags;
```

Try them (seed data: *Laptop Pro* — Electronics, 1299.99, tags `sale`, `new`; *Clean Code* — Books, 34.99, `bestseller`; *Mystery Box* — no category, 9.99, no tags):

```bash
S="http://localhost:8080/api"; J="Content-Type: application/json"

# Case-insensitive contains
curl -X POST "$S/products/search" -H "$J" -d '{"name_like":"laptop"}'
# → [{"name":"Laptop Pro",...}]

# Price range
curl -X POST "$S/products/search" -H "$J" -d '{"price_gte":30,"price_lte":100}'
# → [{"name":"Clean Code",...}]

# By related entity id, and products without a category
curl -X POST "$S/products/search" -H "$J" -d '{"categoryId":1}'
curl -X POST "$S/products/search" -H "$J" -d '{"categoryId_is_null":true}'
# → [{"name":"Mystery Box",...}]

# Collection: has any of / has none of / empty
curl -X POST "$S/products/search" -H "$J" -d '{"tags_in":["sale","bestseller"]}'   # Laptop Pro, Clean Code
curl -X POST "$S/products/search" -H "$J" -d '{"tags_not_in":["sale"]}'            # Clean Code, Mystery Box
curl -X POST "$S/products/search" -H "$J" -d '{"tags_is_null":true}'               # Mystery Box

# No filters: {} (or no body) returns everything
curl -X POST "$S/products/search" -H "$J" -d '{}'

# Orders (Basic auth): search is a read, so the CUSTOMER role may use it — creating an order still needs ADMIN
curl -u customer:customer123 -X POST "$S/orders/search" -H "$J" -d '{"status_not_in":["CANCELLED"]}'
curl -u customer:customer123 -X POST "$S/orders/search" -H "$J" -d '{"productId":1}'

# Orders are pageable — page / size / sort stay in the query string
curl -u customer:customer123 -X POST "$S/orders/search?page=0&size=1&sort=totalAmount,desc" -H "$J" \
  -d '{"status_not_in":["CANCELLED"]}'
# → {"content":[{"reference":"ORD-0001",...}],"totalElements":2,"totalPages":2,"number":0,"size":1, ...}

# A value of the wrong type → 400, keyed by the JSON key
curl -X POST "$S/products/search" -H "$J" -d '{"price_gte":"cheap"}'
# → {"status":400,"errors":{"price_gte":"Cannot deserialize value of type `java.lang.Double` ..."}, ...}

# An unknown key (a typo, or an operation the field doesn't declare) → 400 with the allowed keys
curl -X POST "$S/products/search" -H "$J" -d '{"price_gt":10}'
# → {"status":400,"errors":{"price_gt":"Unknown search key 'price_gt' for Product — allowed keys: name, name_like, price_gte, ..."}, ...}
```

In Swagger UI the search endpoint shows the body as one typed JSON schema (every key with its type and a description) and an `example` body with every search key and a placeholder value of its type — trim it to the keys you want (`{}` returns everything). `page` / `size` / `sort` appear as query parameters on `Order`.

**Why a POST?** Search only reads, but a `GET` cannot reliably carry a request body, and a body keeps the filters typed, has no URL length limit and lets unknown keys be rejected. The read-only, retry-safe HTTP `QUERY` method ([RFC 10008](https://www.rfc-editor.org/rfc/rfc10008)) is made for this and is planned in spring-xpose once Spring supports it, with the same JSON body. All operations, value formats, compile-time checks and the full reasoning: [search filters reference](https://github.com/notablogger/spring-xpose/blob/main/docs/tech/annotation-reference.md#search--post-apipathsearch).

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
    Product.java                  ← @ExposeEntity, public, ALWAYS_OBJECT, hidden field, SEARCH
    Order.java                    ← @ExposeEntity, Basic auth, role-split, pageable, SEARCH
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
