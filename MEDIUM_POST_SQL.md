# Build Spring Boot SQL CRUD APIs Faster with Compile-Time Generation (`spring-xpose`)

If your Spring Boot service has more than a few SQL entities, you already know the pattern:

- define entity
- write repository
- define request/response DTOs
- wire mapper
- write controller
- add validation + OpenAPI + security

Then repeat for every table.

This article shows a practical SQL-first workflow using **`spring-xpose` 3.0.0** where one annotation drives compile-time generation of that repetitive layer.

---

## What you get for SQL entities

From a single `@ExposeEntity` on a JPA entity, `spring-xpose` generates:

- `JpaRepository`
- response DTO
- request DTO
- MapStruct mapper interface
- REST controller
- per-resource `SecurityFilterChain`

All generated code is plain Java in `build/generated/sources/...`, so you can inspect and debug it like normal application code.

---

## Why this helps real teams

For SQL-heavy backends, this cuts down work in three places:

1. **Boilerplate reduction**
   - CRUD endpoints become configuration, not repetitive implementation.

2. **Consistency**
   - endpoint shapes, status codes, security style, and DTO rules are generated uniformly.

3. **Safer iteration**
   - changes happen in one place (annotation/entity), then regenerated artifacts stay in sync.

---

## SQL example: one entity, generated API

```java
@Entity
@ExposeEntity(
    path = "orders",
    expose = {
        Operation.FIND_ALL,
        Operation.FIND_BY_ID,
        Operation.CREATE,
        Operation.UPDATE
    },
    authType = AuthType.BASIC,
    readRoles = {"CUSTOMER", "ADMIN"},
    writeRoles = {"ADMIN"}
)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String reference;

    @PositiveOrZero
    private Double totalAmount;

    private String status;
}
```

Key point: this is SQL/JPA, so generated repository is `JpaRepository<Order, Long>` and generated write endpoints are transactional.

---

## Configurability that matters in production-ish apps

`@ExposeEntity` is not just on/off CRUD. You can tune behavior per entity.

### 1) Exposed operations

Use `expose = {...}` to include only methods you want.

- Remove `Operation.DELETE` to keep data immutable from API side.
- Security generation now follows `expose` (no stale DELETE matcher if DELETE is disabled).

### 2) Security model

- `AuthType.NONE` -> public resource
- `AuthType.BASIC` -> HTTP Basic
- `AuthType.OAUTH2` -> JWT resource server

You can split read/write access:

```java
readRoles = {"USER", "ADMIN"},
writeRoles = {"ADMIN"}
```

### 3) DTO shaping

- `ignoredFields` hides fields from both request and response DTOs.
- validation annotations (`@NotBlank`, `@Positive`, etc.) are copied to **request DTO** for input validation.
- response DTO keeps fields but skips validation annotations.

### 4) Relation serialization

Control response shape with `relationMode`:

- `IDS_FOR_LIST_OBJECT_FOR_SINGLE`
- `ALWAYS_IDS`
- `ALWAYS_OBJECT`

This is useful for balancing payload size and readability in SQL relation graphs.

---

## SQL schema and seed strategy in the sample

In the sample app, relational schema is managed by Hibernate and seed data is inserted via `data.sql`.

- Hibernate creates/updates schema based on entities.
- `data.sql` provides deterministic demo data (`category`, `product`, `orders`, `report`).

That gives fast local startup while keeping explicit seed fixtures.

---

## Generated security is scoped per resource

For `/api/orders`, generated security config applies only to that path:

- GET -> `CUSTOMER` or `ADMIN`
- POST/PUT -> `ADMIN`
- non-exposed methods -> denied

This avoids a giant hand-written security file and keeps access rules close to entity intent.

---

## How to run the SQL flow (sample)

```bash
cd spring-xpose-sample-rest
docker compose up -d postgres mongo
./gradlew bootRun --args='--spring.profiles.active=local'
```

Then test:

```bash
curl -i http://localhost:8080/api/orders
curl -i -u customer:customer123 http://localhost:8080/api/orders
curl -i -u admin:admin123 -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"reference":"ORD-NEW","totalAmount":49.99,"status":"NEW"}'
```

In the current sample configuration, unauthenticated `GET /api/orders` is blocked (`403`) and authenticated customer access returns `200`.

---

## Trade-offs and where it fits best

Best fit:

- internal platforms
- admin APIs
- data-centric services with many entities
- teams that value consistency and speed over handcrafted CRUD

Less fit:

- highly custom orchestration endpoints
- domain workflows where each endpoint is unique by design

---

## Final take

For SQL/JPA services, `spring-xpose` turns repetitive CRUD layers into compile-time configuration while staying transparent and debuggable.

You still control:

- which operations exist
- who can access them
- how DTOs look
- how relations are represented

That’s the key: less repetitive coding, more intentional API design.

---

## Links

- Library: https://github.com/notablogger/spring-xpose
- Sample app: https://github.com/notablogger/spring-xpose-sample-rest
- Maven Central: https://central.sonatype.com/search?q=io.github.notablogger
