# Stop Writing the Same Spring Boot CRUD Code Over and Over — Generate It at Compile Time with `spring-xpose`

> A practical look at using one annotation on a JPA entity to generate repositories, DTOs, mappers, controllers, OpenAPI docs, and per-entity security in Spring Boot 4.

**Suggested subtitle:**
How I used compile-time code generation to remove repetitive REST boilerplate in Spring Boot — while keeping the output fully readable and debuggable.

---

## Suggested Medium packaging

**Suggested title options**

1. **Stop Writing the Same Spring Boot CRUD Code Over and Over — Generate It at Compile Time**
2. **One Annotation, Full REST API: A Compile-Time Approach for Spring Boot**
3. **I Got Tired of Writing Spring CRUD Boilerplate, So I Generated It Instead**

**Suggested tags**

- Spring Boot
- Java
- REST API
- Backend
- Productivity

**What tends to work well on Medium**

- Open with a strong pain point in the first 2–3 sentences
- Keep sections short and scannable
- Use one concrete example instead of too many abstractions
- Include code that readers can copy quickly
- Show the result early, not only the setup
- End with links, repo, and a clear call to action
- Add 1–3 screenshots in the published version:
  - Swagger UI
  - generated sources in the IDE
  - sanitized error response example

---

## The post

If you build Spring Boot APIs often, you’ve probably written the same stack of code more times than you’d like to admit:

- a repository
- a DTO
- a mapper
- a controller
- validation wiring
- OpenAPI annotations
- security rules

Then you do it all again for the next entity.

That repetition is one of the most frustrating parts of building CRUD-heavy applications. The code is not especially hard, but it is noisy, repetitive, and easy to make inconsistent.

That’s the problem I wanted to solve with **[`spring-xpose`](https://github.com/notablogger/spring-xpose)**.

It’s a Spring Boot library that reads a single annotation — `@ExposeEntity` — at **compile time** and generates a complete REST layer for a JPA entity.

Not runtime magic. Not reflection-heavy endpoint registration. Not hidden proxies.

It generates real `.java` files you can open, read, debug, and step through in your IDE.

---

## Why I built it this way

There are already ways to expose data quickly in the Spring ecosystem, but I wanted a few very specific things:

- **compile-time generation**, not runtime behavior hidden behind conventions
- **DTOs and mappers generated automatically**
- **per-entity security** with `NONE`, `BASIC`, or `OAUTH2`
- **role-based read/write split**
- **OpenAPI-ready generated controllers**
- **readable generated code** that shows up in stack traces and can be debugged normally

That last point mattered a lot.

I didn’t want a system where you save boilerplate but lose visibility.
I wanted something closer to how developers already think about **Lombok** or **MapStruct**:

- write a little metadata
- build the project
- inspect the generated source
- keep full control

---

## What `spring-xpose` generates

From one annotated entity, `spring-xpose` generates:

- a Spring Data repository
- a response DTO
- a request DTO
- a MapStruct mapper
- a `@RestController`
- a per-resource `SecurityFilterChain`

So instead of manually wiring the same REST surface again and again, you define the intent once on the entity.

Project links:

- Library: [`spring-xpose`](https://github.com/notablogger/spring-xpose)
- Runnable sample app: [`spring-xpose-sample-rest`](https://github.com/notablogger/spring-xpose-sample-rest)
- Maven Central artifacts: [io.github.notablogger on Maven Central](https://central.sonatype.com/search?q=io.github.notablogger)

---

## A minimal example

Here is the kind of entity setup this approach is built for:

```java
import io.github.notablogger.springxpose.annotation.ExposeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

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

Then you build the project.

```bash
./gradlew build
```

And `spring-xpose` generates the REST API pieces for you.

That includes code under:

```text
build/generated/sources/annotationProcessor/java/main/
```

The generated classes are real source files, not black-box runtime behavior.

---

## What makes the sample project useful

The sample app is where the idea becomes practical:

[`spring-xpose-sample-rest`](https://github.com/notablogger/spring-xpose-sample-rest)

It demonstrates four common scenarios:

| Entity | What it demonstrates |
|---|---|
| `Category` | public CRUD with zero-config access |
| `Product` | relation serialization and hidden fields via `ignoredFields` |
| `Order` | HTTP Basic auth with separate read/write roles |
| `Report` | OAuth2-protected CRUD with local testing support |

This matters because most CRUD generators look fine in a toy example, but become less convincing once you add:

- different auth strategies
- relation handling
- request vs response shapes
- validation
- error handling

The sample shows those concerns working together in a runnable Spring Boot app.

---

## Running the sample locally

The sample project uses:

- **Spring Boot 4.0.6**
- **Java 21**
- **Gradle 8.14+**
- **H2** for local data
- **Swagger UI** for exploring the generated API

Clone and run it:

```bash
git clone https://github.com/notablogger/spring-xpose-sample-rest.git
cd spring-xpose-sample-rest
./gradlew bootRun
```

Useful local URLs:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- H2 Console: <http://localhost:8080/h2-console>

---

## Local OAuth testing without external infrastructure

One friction point with OAuth demos is that they usually depend on a real identity provider or JWKS endpoint.

I wanted the sample to be runnable without that setup overhead.

So the sample includes a local profile that lets you test bearer-token-protected endpoints without standing up a separate auth server.

Run the sample with the local profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Then call the secured report endpoints with a local token:

```bash
curl -i -H 'Authorization: Bearer local-test-token' \
  http://localhost:8080/api/reports
```

That makes the sample much easier to try, especially for readers who want to understand the generated security behavior quickly.

---

## Example: generated API behavior

Here are a few sample calls from the demo app.

### Public endpoint

```bash
curl http://localhost:8080/api/categories
```

### Basic Auth protected endpoint

```bash
curl -u customer:customer123 http://localhost:8080/api/orders
```

### Admin-only write

```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"reference":"ORD-TEST","totalAmount":49.99,"status":"NEW"}'
```

### OAuth-protected endpoint in local profile

```bash
curl -i -H 'Authorization: Bearer local-test-token' \
  http://localhost:8080/api/reports
```

---

## Error handling matters more than code generation demos usually admit

One detail I cared about was **error handling quality**, especially around persistence errors.

A lot of demos happily leak raw SQL exceptions, constraint names, or database internals back to API clients.
That’s not a great default.

`spring-xpose` includes a library-level exception handler for common API cases such as:

- malformed JSON
- bean validation failures
- relation lookups that point to missing entities
- data integrity violations
- optimistic locking conflicts

The sample app goes one step further and **overrides the constraint violation handler locally** so the API returns a sanitized payload instead of raw SQL details.

Example response:

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

That gives API clients a clean, stable contract while keeping full details in server logs.

---

## Exceptions handled by the library

The default `spring-xpose` exception handler currently covers:

| Exception type | Status | Problem type |
|---|---:|---|
| `HttpMessageNotReadableException` | `400` | `urn:springxpose:malformed-body` |
| `MethodArgumentNotValidException` | `400` | `urn:springxpose:validation-error` |
| `EntityNotFoundException` | `422` | `urn:springxpose:relation-not-found` |
| `DataIntegrityViolationException` | `409` | `urn:springxpose:constraint-violation` |
| `OptimisticLockingFailureException` | `409` | `urn:springxpose:optimistic-lock` |

If you want custom behavior in your own app, you can override the library defaults with your own `@RestControllerAdvice`, just like the sample does.

---

## Why compile-time generation instead of runtime magic?

This is probably the main design choice.

For me, compile-time generation has several advantages:

### 1. The output is visible

You can inspect the generated code directly in:

```text
build/generated/sources/annotationProcessor/java/main/
```

That means no guessing what the framework is doing.

### 2. It is easier to debug

Generated classes are real Java classes.
You can set breakpoints, inspect stack traces, and step through requests normally.

### 3. It keeps the contract explicit

You are not dynamically exposing your entity model at runtime through hidden conventions.
The generated layer is concrete and reviewable.

### 4. It scales better for teams

When teams have many entities, consistency matters.
Compile-time generation gives you a repeatable pattern without hiding the actual implementation.

---

## Where this fits best

I think `spring-xpose` is most useful when:

- you have many CRUD-style entities
- you still want DTOs, validation, and explicit controllers
- you want security defined per entity
- you care about generated code being readable
- you want to move faster without giving up control

It is probably less useful if your API is mostly bespoke orchestration logic, workflow-heavy endpoints, or highly customized domain behavior from day one.

But for admin APIs, internal platforms, dashboards, and data-centric services, it can remove a surprising amount of repetitive code.

---

## Quick start links

If you want to try it yourself:

- **Library repo:** <https://github.com/notablogger/spring-xpose>
- **Sample app:** <https://github.com/notablogger/spring-xpose-sample-rest>
- **Maven Central:** <https://central.sonatype.com/search?q=io.github.notablogger>
- **Annotation reference:** <https://github.com/notablogger/spring-xpose/blob/main/docs/tech/annotation-reference.md>
- **Configuration docs:** <https://github.com/notablogger/spring-xpose/blob/main/docs/tech/configuration.md>
- **Architecture docs:** <https://github.com/notablogger/spring-xpose/blob/main/docs/tech/architecture.md>
- **Generator guide:** <https://github.com/notablogger/spring-xpose/blob/main/docs/tech/generator-guide.md>
- **Generated artifacts reference:** <https://github.com/notablogger/spring-xpose/blob/main/docs/tech/generated-artifacts.md>

---

## Closing thought

I didn’t build `spring-xpose` because writing Spring controllers is impossible.
I built it because writing the same controller shape, DTO shape, mapper shape, security rules, and OpenAPI annotations over and over again is a poor use of time.

If a machine can generate the repetitive part at compile time, and still leave me with code I can read and debug, that feels like a better tradeoff.

If that resonates with you, the sample app is the best place to start:

**[`spring-xpose-sample-rest`](https://github.com/notablogger/spring-xpose-sample-rest)**

---

## Optional author note for Medium

If you want a more personal ending, you can add something like this:

> I’m still evolving the project, especially around developer experience, generated artifacts, and future directions like GraphQL. If you try it, I’d love feedback on where compile-time API generation helps most — and where it still gets in the way.

---

## Optional publish checklist

Before publishing on Medium, I’d recommend:

- adding a screenshot of Swagger UI
- adding a screenshot of generated code in IntelliJ
- adding one screenshot of the sanitized `409` error payload
- keeping the final post around a 5–8 minute read
- bolding only a few key phrases, not every paragraph
- using one main CTA at the end, not several competing ones

