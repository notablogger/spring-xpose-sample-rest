# spring-xpose-sample-rest

A standalone Spring Boot sample demonstrating every configuration variant
supported by **spring-xpose** over a REST API.

## Entity matrix

| Entity | Auth | Operations | Relation mode | Notes |
|---|---|---|---|---|
| `Category` | NONE | ALL | — | No-auth, full CRUD, has OneToMany to Product (not serialised outward) |
| `Product` | NONE | ALL | `ALWAYS_OBJECT` | Relation to Category always returns full object |
| `Author` | NONE | ALL | — | No-auth, full CRUD, no outward relations |
| `Article` | NONE | FIND_ALL, FIND_BY_ID | `ALWAYS_IDS` | Read-only, relation to Author always returns ID only |
| `Order` | **BASIC** | ALL | default (`IDS_FOR_LIST_OBJECT_FOR_SINGLE`) | readRoles=CUSTOMER,ADMIN · writeRoles=ADMIN |
| `Report` | **OAUTH2** | ALL | — | readRoles=ANALYST,ADMIN · writeRoles=ADMIN · no relations |
| `Tag` | NONE | FIND_ALL, FIND_BY_ID | — | Minimal read-only catalogue, no relations |

## Running

```bash
./gradlew bootRun
```

| URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/h2-console | H2 web console (JDBC: `jdbc:h2:mem:restdb`) |

## Basic-auth users (for `/api/orders`)

| Username | Password | Role |
|---|---|---|
| `customer` | `customer123` | `ROLE_CUSTOMER` — read only (GET) |
| `admin` | `admin123` | `ROLE_ADMIN` — full CRUD |

## OAuth2 (for `/api/reports`)

Send a Bearer JWT with `scope` claim containing `ANALYST` (read) or `ADMIN` (write).  
Set `spring.security.oauth2.resourceserver.jwt.jwks-uri` in `application.properties` to point at your IdP.

