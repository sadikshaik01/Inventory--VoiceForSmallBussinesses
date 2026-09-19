package com.voicestock.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "products")
public class Product extends AuditedEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotBlank @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String name;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String category;

    @NotNull
    @Column(nullable = false, length = 20)
    private InventoryUnit unit;

    @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 3)
    @Column(name = "current_stock", nullable = false, precision = 19, scale = 3)
    private BigDecimal currentStock;

    @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 3)
    @Column(name = "minimum_stock", nullable = false, precision = 19, scale = 3)
    private BigDecimal minimumStock;

    @DecimalMin("0") @Digits(integer = 17, fraction = 2)
    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean archived;

    protected Product() {}

    public Product(User user, String name, String category, InventoryUnit unit,
                   BigDecimal currentStock, BigDecimal minimumStock, BigDecimal price) {
        this.user = user;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.currentStock = currentStock;
        this.minimumStock = minimumStock;
        this.price = price;
    }

    public User getUser() { return user; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public InventoryUnit getUnit() { return unit; }
    public BigDecimal getCurrentStock() { return currentStock; }
    public BigDecimal getMinimumStock() { return minimumStock; }
    public BigDecimal getPrice() { return price; }
    public void setName(String name) { this.name = name; }
    public boolean isArchived() { return archived; }
    public void archive() { archived = true; }
    public void setCurrentStock(BigDecimal stock) { this.currentStock = stock; }
    public void updateDetails(String name, String category, InventoryUnit unit, BigDecimal minimumStock, BigDecimal price) {
        this.name = name; this.category = category; this.unit = unit;
        this.minimumStock = minimumStock; this.price = price;
    }
}
