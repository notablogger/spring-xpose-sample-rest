package io.github.springxpose.sample.rest.entity;

import io.github.springxpose.annotation.AuthType;
import io.github.springxpose.annotation.ExposeEntity;
import io.github.springxpose.annotation.Operation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Order — full CRUD, secured with HTTP Basic auth.
 * readRoles = CUSTOMER, ADMIN
 * writeRoles = ADMIN only
 * Relation to Product rendered as IDS_FOR_LIST_OBJECT_FOR_SINGLE (default).
 */
@Entity
@Table(name = "orders")
@ExposeEntity(
    path = "orders",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID, Operation.CREATE, Operation.UPDATE, Operation.DELETE},
    authType = AuthType.BASIC,
    readRoles  = {"CUSTOMER", "ADMIN"},
    writeRoles = {"ADMIN"}
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String reference;

    @PositiveOrZero
    private Double totalAmount;

    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}

