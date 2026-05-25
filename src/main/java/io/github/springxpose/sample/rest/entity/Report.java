package io.github.springxpose.sample.rest.entity;

import io.github.springxpose.annotation.AuthType;
import io.github.springxpose.annotation.ExposeEntity;
import io.github.springxpose.annotation.Operation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Report — full CRUD, secured with OAuth2 (JWT bearer token).
 * readRoles  = ANALYST, ADMIN
 * writeRoles = ADMIN only
 * No relations.
 */
@Entity
@ExposeEntity(
    path = "reports",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID, Operation.CREATE, Operation.UPDATE, Operation.DELETE},
    authType   = AuthType.OAUTH2,
    readRoles  = {"ANALYST", "ADMIN"},
    writeRoles = {"ADMIN"}
)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String summary;

    private String generatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
}

