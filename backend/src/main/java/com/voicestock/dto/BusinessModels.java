package com.voicestock.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import com.voicestock.dto.ApiModels.ProductView;
import com.voicestock.entity.*;

public final class BusinessModels {
    private BusinessModels() {}
    public record Insight(ProductView product,String status,BigDecimal recentUnitsRemoved,BigDecimal observedDays,
        BigDecimal averageDailyUsage,BigDecimal daysRemaining,BigDecimal suggestedReorderQuantity,String reason) {}
    public record ActionView(UUID id,UUID productId,String productName,InventoryUnit unit,BigDecimal recommendedQuantity,
        String status,String reason,Instant createdAt,Instant approvedAt,Instant completedAt,boolean productArchived) {
        public static ActionView of(BusinessAction a) { return new ActionView(a.getId(),a.getProduct().getId(),a.getProduct().getName(),a.getUnit(),a.getRecommendedQuantity(),a.getStatus(),a.getReason(),a.getCreatedAt(),a.getApprovedAt(),a.getCompletedAt(),a.getProduct().isArchived()); }
    }
    public record Center(List<Insight> insights,List<ActionView> actions,long lowStock,long outOfStock,long runningOutSoon) {}
}

