# I Got Tired of Writing Spring Boot CRUD Boilerplate, So I Generated It at Compile Time

If you build Spring Boot APIs often, you’ve probably written the same pieces again and again:

- repository
- DTO
- mapper
- controller
- validation handling
- security rules
- OpenAPI annotations

That repetition is why I built **[`spring-xpose`](https://github.com/notablogger/spring-xpose)**.

It lets you annotate a JPA entity once and generate a full REST layer at **compile time** — as real `.java` files you can open, read, and debug.

---

## 1. Why we did it

The goal was simple: remove repetitive CRUD boilerplate without hiding what the framework is doing.

I wanted something that was:

- compile-time, not runtime magic
- visible in generated source
- easy to debug in a normal IDE
- practical enough to include DTOs, security, and OpenAPI support

For CRUD-heavy services, the repetitive part is predictable. That makes it a good fit for code generation.

---

## 2. How it works

You annotate a JPA entity with `@ExposeEntity`.

```java
@Entity
@ExposeEntity(path = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Positive
    private Double price;
}
```

At build time, `spring-xpose` generates:

- repository
- response DTO
- request DTO
- MapStruct mapper
- `@RestController`
- per-resource `SecurityFilterChain`

Generated files appear under:

```text
build/generated/sources/annotationProcessor/java/main/
```

So instead of wiring the same REST stack by hand, you define intent once and inspect the output normally.

---

## 3. Links + steps to use it

Project links:

- Library: <https://github.com/notablogger/spring-xpose>
- Sample app: <https://github.com/notablogger/spring-xpose-sample-rest>
- Maven Central: <https://central.sonatype.com/search?q=io.github.notablogger>
- Annotation reference: <https://github.com/notablogger/spring-xpose/blob/main/docs/tech/annotation-reference.md>
- Configuration docs: <https://github.com/notablogger/spring-xpose/blob/main/docs/tech/configuration.md>

The sample app demonstrates:

| Entity | What it shows |
|---|---|
| `Category` | public CRUD |
| `Product` | hidden fields and relation serialization |
| `Order` | HTTP Basic with separate read/write roles |
| `Report` | OAuth2-protected CRUD |

Try the sample locally:

```bash
git clone https://github.com/notablogger/spring-xpose-sample-rest.git
cd spring-xpose-sample-rest
./gradlew bootRun
```

Useful URLs:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- H2 Console: <http://localhost:8080/h2-console>

To test OAuth-protected endpoints locally without external auth setup:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'

curl -i -H 'Authorization: Bearer local-test-token' \
  http://localhost:8080/api/reports
```

---

## 4. Benefits

The main benefits for me are:

- less repetitive CRUD code
- generated code that stays visible and reviewable
- normal debugging with real classes and stack traces
- built-in support for DTOs, security, and OpenAPI
- safer API behavior, including sanitized constraint-violation responses in the sample app

Example sanitized error response:

```json
{
  "type": "urn:springxpose:constraint-violation",
  "title": "Data integrity violation",
  "status": 409,
  "detail": "The request conflicts with existing data. Verify referenced IDs and unique values.",
  "instance": "/api/orders",
  "errorCode": "CONSTRAINT_VIOLATION"
}
```

That’s the core idea behind `spring-xpose`: less boilerplate, but without giving up control.

