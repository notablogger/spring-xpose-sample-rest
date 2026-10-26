package io.github.notablogger.springxpose.sample.rest.entity;

import io.github.notablogger.springxpose.annotation.AuthType;
import io.github.notablogger.springxpose.annotation.ExposeEntity;
import io.github.notablogger.springxpose.annotation.ExposeField;
import io.github.notablogger.springxpose.annotation.Operation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import static io.github.notablogger.springxpose.annotation.FilterOperation.*;

@Entity
@Table(name = "orders")
@ExposeEntity(
    path = "orders",
    expose = {Operation.FIND_ALL, Operation.FIND_BY_ID, Operation.CREATE, Operation.UPDATE,
              Operation.SEARCH},                                     // POST /api/orders/search (read roles)
    authType = AuthType.BASIC,
    readRoles  = {"CUSTOMER", "ADMIN"},
    writeRoles = {"ADMIN"},
    pageable   = true                                            // findAll and search: ?page=0&size=20&sort=totalAmount,desc
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @ExposeField(filterOperations = LIKE)                        // {"reference_like": "ord-00"}
    private String reference;

    @PositiveOrZero
    @ExposeField(filterOperations = {GTE, LTE})                  // {"totalAmount_gte": 100}
    private Double totalAmount;

    @ExposeField(filterOperations = {EQ, IN, NOT_IN})            // {"status": "PENDING"}  {"status_not_in": ["CANCELLED"]}
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    @ExposeField(filterOperations = EQ)                          // {"productId": 1}
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

