package io.github.springxpose.sample.rest.entity;

import io.github.springxpose.annotation.AuthType;
import io.github.springxpose.annotation.ExposeEntity;
import io.github.springxpose.annotation.Operation;
import io.github.springxpose.annotation.RelationMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Article — read operations only (FIND_ALL, FIND_BY_ID), no auth.
 * Relation to Author rendered as IDs only in all contexts (RelationMode.ALWAYS_IDS).
 */
@Entity
@ExposeEntity(
    path = "articles",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID},
    relationMode = RelationMode.ALWAYS_IDS
)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String content;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    private Author author;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
}

