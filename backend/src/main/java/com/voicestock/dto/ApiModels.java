package com.voicestock.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;
import com.voicestock.entity.*;
import org.springframework.data.domain.Page;

public final class ApiModels {
    private ApiModels() {}

    public record RegisterRequest(@NotBlank @Size(max=120) String name,
        @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=8, max=72) String password,
        @NotBlank @Size(max=160) String businessName, @NotBlank @Pattern(regexp="en|te|hi|mixed") String preferredLanguage) {}
    public record LoginRequest(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=72) String password) {}
    public record ProfileRequest(@NotBlank @Size(max=120) String name, @NotBlank @Size(max=160) String businessName,
        @NotBlank @Pattern(regexp="en|te|hi|mixed") String preferredLanguage) {}
    public record UserView(UUID id, String name, String email, String businessName, String preferredLanguage) {
        public static UserView of(User u) { return new UserView(u.getId(), u.getName(), u.getEmail(), u.getBusinessName(), u.getPreferredLanguage()); }
    }
    public record AuthResponse(String token, UserView user) {}
    public record ProductCreate(@NotBlank @Size(max=160) String name, @NotBlank @Size(max=100) String category,
        @NotNull InventoryUnit unit, @NotNull @DecimalMin("0") @Digits(integer=16,fraction=3) BigDecimal currentStock,
        @NotNull @DecimalMin("0") @Digits(integer=16,fraction=3) BigDecimal minimumStock,
        @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal price) {}
    public record ProductEdit(@NotBlank @Size(max=160) String name, @NotBlank @Size(max=100) String category,
        @NotNull InventoryUnit unit, @NotNull @DecimalMin("0") @Digits(integer=16,fraction=3) BigDecimal minimumStock,
        @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal price) {}
    public record StockRequest(@NotNull UUID productId, @NotNull @DecimalMin(value="0", inclusive=false)
        @Digits(integer=16,fraction=3) BigDecimal quantity, @NotNull InventoryUnit unit) {}
    public record ProductView(UUID id, String name, String category, InventoryUnit unit, BigDecimal currentStock,
        BigDecimal minimumStock, BigDecimal price, String status, BigDecimal reorderQuantity, boolean archived, Instant updatedAt) {
        public static ProductView of(Product p) {
            String status = p.getCurrentStock().signum()==0 ? "OUT_OF_STOCK" :
                p.getCurrentStock().compareTo(p.getMinimumStock())<=0 ? "LOW_STOCK" : "NORMAL";
            BigDecimal target = p.getMinimumStock().multiply(BigDecimal.valueOf(2)).max(p.getMinimumStock().add(BigDecimal.ONE));
            BigDecimal reorder = status.equals("NORMAL") ? BigDecimal.ZERO : target.subtract(p.getCurrentStock()).max(BigDecimal.ZERO);
            return new ProductView(p.getId(),p.getName(),p.getCategory(),p.getUnit(),p.getCurrentStock(),p.getMinimumStock(),p.getPrice(),
                status,reorder,p.isArchived(),p.getUpdatedAt());
        }
    }
    public record TransactionView(UUID id, UUID productId, String productName, TransactionType type, BigDecimal quantity,
        InventoryUnit unit, TransactionSource source, Instant createdAt) {
        public static TransactionView of(InventoryTransaction t) {
            return new TransactionView(t.getId(),t.getProduct().getId(),t.getProduct().getName(),t.getTransactionType(),
                t.getQuantity(),t.getUnit(),t.getSource(),t.getCreatedAt());
        }
    }
    public record PageView<T>(List<T> items, int page, int totalPages, long totalElements) {
        public static <T> PageView<T> of(Page<T> p) { return new PageView<>(p.getContent(),p.getNumber(),p.getTotalPages(),p.getTotalElements()); }
    }
    public record DashboardView(long totalProducts, long lowStock, long outOfStock, long todaysTransactions, List<TransactionView> recentTransactions) {}
}
