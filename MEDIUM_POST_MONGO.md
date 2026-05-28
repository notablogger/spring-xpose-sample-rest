# Build MongoDB APIs in Spring Boot with Less Boilerplate (`spring-xpose` + `@ExposeDocument`)

Spring Data MongoDB is productive, but for CRUD-heavy services you still end up writing the same application layer repeatedly:

- repository
- request/response DTOs
- mapper
- controller
- security configuration
- OpenAPI annotations

This article shows how `spring-xpose` eliminates that repetition for MongoDB resources using **`@ExposeDocument`** — the MongoDB-native annotation that builds a full CRUD REST layer at compile time.

---

## `@ExposeDocument` vs `@ExposeEntity`

spring-xpose provides two annotations — you pick the right one for your persistence backend:

| | `@ExposeDocument` | `@ExposeEntity` |
|---|---|---|
| Backend | MongoDB | JPA (SQL) |
| Generated repository | `MongoRepository` | `JpaRepository` |
| `EntityManager` injection | ❌ | ✅ |
| `@Transactional` on writes | ❌ | ✅ |
| JPA relation support | ❌ | ✅ |
| `relationMode` attribute | ❌ | ✅ |

---

## What changes with MongoDB mode

Both annotations drive the same generation switches from JPA-style to Mongo-style:

- repository → `MongoRepository<Entity, Id>`
- no `EntityManager` usage
- no JPA transaction annotations generated for write methods
- Mongo `@Id` support (`org.springframework.data.annotation.Id`)

---

## MongoDB example in the sample

```java
@Document(collection = "notes")
@ExposeDocument(
    path       = "notes",
    authType   = AuthType.BASIC,
    readRoles  = {"CUSTOMER", "ADMIN"},
    writeRoles = {"ADMIN"}
)
public class Note {
    @Id                   // @org.springframework.data.annotation.Id
    private String id;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String author;
}
```

From this, spring-xpose generates:

- `NoteRepository extends MongoRepository<Note, String>`
- `NoteDto` / `NoteRequestDto`
- `NoteMapper`
- `NoteController`
- `NoteSecurityConfigurer`

---


## Why this helps Mongo projects

### 1) Same developer experience across SQL and Mongo

Teams often run mixed persistence (SQL for transactional domains, Mongo for document domains). `spring-xpose` keeps the top-level API pattern consistent while generating datastore-specific internals.

### 2) Faster onboarding

New contributors read one entity annotation and immediately understand endpoint shape and security rules. `@ExposeDocument` makes it unambiguous that this class is a MongoDB document.

### 3) Easy per-resource policy

MongoDB resources can still be:

- public (`AuthType.NONE`)
- basic-auth protected (`AuthType.BASIC`)
- OAuth2 protected (`AuthType.OAUTH2`)

and use read/write role split exactly like SQL entities.

---

## Mongo seeding in the sample

Mongo sample data is seeded through Docker init script:

- file: `docker/mongo/init-notes.js`
- mounted into: `/docker-entrypoint-initdb.d/init-notes.js`
- inserts demo notes only when collection is empty

```javascript
db = db.getSiblingDB("xpose");

if (db.notes.countDocuments() === 0) {
  db.notes.insertMany([
    { title: "Welcome Note", content: "...", author: "system" },
    { title: "Real Mongo", content: "...", author: "system" }
  ]);
}
```

---

## Generated API behavior (Mongo)

Endpoints generated for `Note`:

- `GET /api/notes`
- `GET /api/notes/{id}`
- `POST /api/notes`
- `PUT /api/notes/{id}`
- `DELETE /api/notes/{id}`

Security is generated from annotation config. Unauthenticated `GET /api/notes` is blocked (`403`); authenticated customer access returns `200`.

---

## Configurability still applies in Mongo mode

Even with `@ExposeDocument`, you keep all core controls:

- `expose` → choose operations
- `ignoredFields` → hide fields from request/response DTOs
- `customMapper` → supply your own mapping bean
- `authType` + role split → enforce policy per endpoint
- `pageable` → paginated `findAll`

So MongoDB support is not a reduced feature path — it's the same model adapted to a document store.

---

## SQL + Mongo in one app

With both `@ExposeEntity` (JPA) and `@ExposeDocument` (Mongo), one service can expose:

- transactional SQL resources (`Order`, `Report`, etc.)
- document resources (`Note`)

without duplicating implementation patterns per datastore.

---

## Run the Mongo flow (sample)

```bash
cd spring-xpose-sample-rest
docker compose up -d postgres mongo
./gradlew bootRun --args='--spring.profiles.active=local'
```

Test `Note` endpoints:

```bash
curl -i http://localhost:8080/api/notes
curl -i -u customer:customer123 http://localhost:8080/api/notes
curl -i -u admin:admin123 -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -d '{"title":"Runtime Note","content":"Created from curl","author":"admin"}'
```

---

## Final take

`@ExposeDocument` in `spring-xpose` 3.0.0 gives MongoDB document classes their own first-class annotation — aligned with the MongoDB vocabulary and free of misleading JPA terminology. For teams shipping many CRUD endpoints across mixed persistence stacks, this keeps API and security conventions consistent and immediately readable.

---

## Links

- Library: https://github.com/notablogger/spring-xpose
- Sample app: https://github.com/notablogger/spring-xpose-sample-rest
- Maven Central: https://central.sonatype.com/search?q=io.github.notablogger
