package io.github.springxpose.sample.rest.entity;

import io.github.springxpose.annotation.ExposeEntity;
import io.github.springxpose.annotation.Operation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Author — exposed with ALL operations, no auth.
 * Has OneToMany to Article (relation not serialized outward — lazy).
 */
@Entity
@ExposeEntity(
    path = "authors",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID, Operation.CREATE, Operation.UPDATE, Operation.DELETE}
)
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String bio;

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<Article> articles;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<Article> getArticles() { return articles; }
    public void setArticles(List<Article> articles) { this.articles = articles; }
}

