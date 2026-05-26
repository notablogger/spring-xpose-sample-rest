package io.github.notablogger.springxpose.sample.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.notablogger.springxpose.annotation.ExposeEntity;
import io.github.notablogger.springxpose.annotation.Operation;
import io.github.notablogger.springxpose.sample.rest.mapper.CustomCategoryMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Entity
@ExposeEntity(
    path = "categories",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID, Operation.CREATE, Operation.UPDATE, Operation.DELETE},
    customMapper = CustomCategoryMapper.class
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String description;

    @JsonIgnore
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}
