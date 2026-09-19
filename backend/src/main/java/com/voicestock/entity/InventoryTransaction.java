package com.voicestock.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction extends BaseEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10, updatable = false)
    private TransactionType transactionType;

    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 3)
    @Column(nullable = false, precision = 19, scale = 3, updatable = false)
    private BigDecimal quantity;

    @NotNull
    @Column(nullable = false, length = 20, updatable = false)
    private InventoryUnit unit;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TransactionSource source;

    protected InventoryTransaction() {}

    public InventoryTransaction(User user, Product product, TransactionType transactionType,
                                BigDecimal quantity, InventoryUnit unit, TransactionSource source) {
        this.user = user;
        this.product = product;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.unit = unit;
        this.source = source;
    }

    public User getUser() { return user; }
    public Product getProduct() { return product; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getQuantity() { return quantity; }
    public InventoryUnit getUnit() { return unit; }
    public TransactionSource getSource() { return source; }
}
