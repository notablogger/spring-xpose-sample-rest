package io.github.springxpose.sample.rest.entity;

import io.github.springxpose.annotation.AuthType;
import io.github.springxpose.annotation.ExposeEntity;
import io.github.springxpose.annotation.Operation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Tag — READ-ONLY (FIND_ALL + FIND_BY_ID), no auth, no relations.
 * Demonstrates a minimal "public catalogue" entity.
 */
@Entity
@ExposeEntity(
    path = "tags",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID}
)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String colour;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }
}

