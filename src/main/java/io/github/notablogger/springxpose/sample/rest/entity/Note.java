package io.github.notablogger.springxpose.sample.rest.entity;

import io.github.notablogger.springxpose.annotation.AuthType;
import io.github.notablogger.springxpose.annotation.ExposeDocument;
import io.github.notablogger.springxpose.annotation.ExposeField;
import io.github.notablogger.springxpose.annotation.FilterOperation;
import io.github.notablogger.springxpose.annotation.Operation;
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
 *   <li>{@code @ExposeField} filters on {@code POST /api/notes/search} run as MongoDB {@code Criteria} queries.</li>
 * </ul>
 */
@Document(collection = "notes")
@ExposeDocument(
    path       = "notes",
    expose     = {Operation.FIND_ALL, Operation.FIND_BY_ID, Operation.CREATE, Operation.UPDATE, Operation.DELETE,
                  Operation.SEARCH},                                 // POST /api/notes/search
    authType   = AuthType.BASIC,
    readRoles  = {"CUSTOMER", "ADMIN"},
    writeRoles = {"ADMIN"}
)
public class Note {

    @Id
    private String id;

    @NotBlank
    @ExposeField(filterOperations = FilterOperation.LIKE)        // {"title_like": "release"}
    private String title;

    @NotBlank
    private String content;

    @ExposeField(filterOperations = {FilterOperation.EQ, FilterOperation.IN})   // {"author": "alice"}  {"author_in": ["alice", "bob"]}
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

