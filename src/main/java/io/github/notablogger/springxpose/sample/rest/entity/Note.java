package io.github.notablogger.springxpose.sample.rest.entity;

import io.github.notablogger.springxpose.annotation.AuthType;
import io.github.notablogger.springxpose.annotation.ExposeDocument;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Sample MongoDB document class exposed via spring-xpose.
 *
 * <p>Demonstrates {@code @ExposeDocument} — the MongoDB-native annotation:
 * <ul>
 *   <li>Uses {@code @org.springframework.data.annotation.Id} (not Jakarta Persistence).</li>
 *   <li>spring-xpose generates a {@code MongoRepository} instead of {@code JpaRepository}.</li>
 *   <li>No {@code EntityManager} is injected — no JPA transactions.</li>
 * </ul>
 */
@Document(collection = "notes")
@ExposeDocument(
    path       = "notes",
    authType   = AuthType.BASIC,
    readRoles  = {"CUSTOMER", "ADMIN"},
    writeRoles = {"ADMIN"}
)
public class Note {

    @Id
    private String id;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String author;

    // ── Getters / Setters ──────────────────────────────────────────────────

    public String getId()           { return id; }
    public void   setId(String id)  { this.id = id; }

    public String getTitle()              { return title; }
    public void   setTitle(String title)  { this.title = title; }

    public String getContent()                { return content; }
    public void   setContent(String content)  { this.content = content; }

    public String getAuthor()               { return author; }
    public void   setAuthor(String author)  { this.author = author; }
}

