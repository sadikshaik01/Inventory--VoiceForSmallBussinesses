package com.voicestock.entity;
import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;

@Entity @Table(name="business_actions")
public class BusinessAction extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,updatable=false) private User user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id",nullable=false,updatable=false) private Product product;
    @Column(name="action_type",nullable=false,length=20,updatable=false) private String actionType="REORDER";
    @Column(name="recommended_quantity",nullable=false,precision=19,scale=3,updatable=false) private BigDecimal recommendedQuantity;
    @Column(nullable=false,length=20,updatable=false) private InventoryUnit unit;
    @Column(nullable=false,length=20) private String status="APPROVED";
    @Column(nullable=false,length=2000,updatable=false) private String reason;
    @Column(name="approved_at",updatable=false) private Instant approvedAt;
    @Column(name="completed_at") private Instant completedAt;
    protected BusinessAction() {}
    public BusinessAction(Product product,BigDecimal quantity,String reason) { this.product=product;this.user=product.getUser();this.unit=product.getUnit();this.recommendedQuantity=quantity;this.reason=reason;this.approvedAt=Instant.now(); }
    public Product getProduct() { return product; }
    public InventoryUnit getUnit() { return unit; }
    public BigDecimal getRecommendedQuantity() { return recommendedQuantity; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void complete() { status="COMPLETED";completedAt=Instant.now(); }
    public void cancel() { status="CANCELLED"; }
}
