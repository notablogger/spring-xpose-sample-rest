package io.github.notablogger.springxpose.sample.rest.entity;

import io.github.notablogger.springxpose.annotation.ExposeEntity;
import io.github.notablogger.springxpose.annotation.ExposeField;
import io.github.notablogger.springxpose.annotation.Operation;
import io.github.notablogger.springxpose.annotation.RelationMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.List;

import static io.github.notablogger.springxpose.annotation.FilterOperation.*;

@Entity
@ExposeEntity(
    path = "products",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID, Operation.CREATE, Operation.UPDATE, Operation.DELETE,
              Operation.SEARCH},                                     // POST /api/products/search
    relationMode = RelationMode.ALWAYS_OBJECT
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @ExposeField(filterOperations = {EQ, LIKE})                  // {"name": "Clean Code"}  {"name_like": "laptop"}
    private String name;

    @Positive
    @ExposeField(filterOperations = {GTE, LTE})                  // {"price_gte": 50, "price_lte": 500}
    private Double price;

    @ExposeField(hidden = true)                                  // stored in DB, never returned or accepted by the API
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    @ExposeField(filterOperations = {EQ, IN, IS_NULL})           // {"categoryId": 1}  {"categoryId_in": [1, 2]}  {"categoryId_is_null": true}
    private Category category;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    @ExposeField(filterOperations = {IN, NOT_IN, IS_NULL})       // {"tags_in": ["sale", "new"]}  {"tags_not_in": ["refurbished"]}  {"tags_is_null": true}
    private List<String> tags = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
