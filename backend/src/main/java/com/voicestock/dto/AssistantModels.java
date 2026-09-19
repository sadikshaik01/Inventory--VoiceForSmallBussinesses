package com.voicestock.dto;

import java.math.BigDecimal;
import java.util.*;
import jakarta.validation.constraints.*;
import com.voicestock.dto.ApiModels.ProductView;
import com.voicestock.dto.BusinessModels.*;

public final class AssistantModels {
    private AssistantModels() {}
    public enum Intent { ADD_STOCK, REMOVE_STOCK, CHECK_STOCK, LOW_STOCK, OUT_OF_STOCK, REORDER, ATTENTION, RUNNING_OUT, STOCK_DURATION, REORDER_REASON, PENDING_ACTIONS, APPROVED_REORDERS, UNKNOWN }
    public record Command(@NotNull Intent intent, @Size(max=160) String product,
        @Digits(integer=16,fraction=3) BigDecimal quantity, @Size(max=40) String unit,
        @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal price,
        @NotNull @Pattern(regexp="en|te|hi|mixed") String language,
        @DecimalMin("0") @DecimalMax("1") BigDecimal confidence,
        List<String> missingFields, UUID productId) {
        public Command(Intent intent,String product,BigDecimal quantity,String unit,BigDecimal price,String language,BigDecimal confidence) {
            this(intent,product,quantity,unit,price,language,confidence,List.of(),null);
        }
        public Command resolved(ProductView p) { return new Command(intent,p.name(),quantity,unit,price,language,confidence,missingFields,p.id()); }
    }
    public record Input(@NotBlank @Size(max=500) String text) {}
    public record Reply(Command command, String message, UUID confirmationId, ProductView product,
        BigDecimal afterStock, List<ProductView> products, List<ProductView> candidates, List<Insight> insights, List<ActionView> actions) {
        public Reply(Command command,String message,UUID confirmationId,ProductView product,BigDecimal afterStock,List<ProductView> products) {
            this(command,message,confirmationId,product,afterStock,products,List.of(),List.of(),List.of());
        }
    }
}
