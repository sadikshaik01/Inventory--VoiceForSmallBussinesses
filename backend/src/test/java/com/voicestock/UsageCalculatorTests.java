package com.voicestock;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.voicestock.dto.ApiModels.ProductView;
import com.voicestock.entity.InventoryUnit;
import com.voicestock.service.UsageCalculator;
import static org.assertj.core.api.Assertions.*;

class UsageCalculatorTests {
    final Instant now=Instant.parse("2026-09-19T00:00:00Z");
    ProductView product(int stock,int min) { return new ProductView(UUID.randomUUID(),"Rice","Grocery",InventoryUnit.BAGS,BigDecimal.valueOf(stock),BigDecimal.valueOf(min),null,"NORMAL",stock<=min?BigDecimal.valueOf(Math.max(min*2,min+1)-stock):BigDecimal.ZERO,false,now); }
    @Test void usageAndRunOutAreTransparent() { var i=UsageCalculator.calculate(product(12,10),now.minus(Duration.ofDays(8)),now,new BigDecimal("28"),2);assertThat(i.averageDailyUsage()).isEqualByComparingTo("4");assertThat(i.daysRemaining()).isEqualByComparingTo("3");assertThat(i.status()).isEqualTo("RUNNING_OUT_SOON");assertThat(i.suggestedReorderQuantity()).isEqualByComparingTo("16");assertThat(i.reason()).contains("28 bags","7 observed days","approximately 3 days"); }
    @Test void insufficientHistoryHasNoPrediction() { var i=UsageCalculator.calculate(product(4,5),now.minus(Duration.ofDays(8)),now,BigDecimal.ZERO,0);assertThat(i.daysRemaining()).isNull();assertThat(i.averageDailyUsage()).isNull();assertThat(i.status()).isEqualTo("LOW_STOCK");assertThat(i.suggestedReorderQuantity()).isEqualByComparingTo("6"); }
    @Test void singleDateAndNewProductsDoNotPredict() { assertThat(UsageCalculator.calculate(product(12,10),now.minus(Duration.ofDays(8)),now,new BigDecimal("28"),1).daysRemaining()).isNull();assertThat(UsageCalculator.calculate(product(12,10),now.minusSeconds(3600),now,new BigDecimal("28"),2).daysRemaining()).isNull(); }
    @Test void outOfStockTakesPrecedence() { var i=UsageCalculator.calculate(product(0,5),now.minus(Duration.ofDays(8)),now,new BigDecimal("28"),2);assertThat(i.status()).isEqualTo("OUT_OF_STOCK");assertThat(i.suggestedReorderQuantity()).isEqualByComparingTo("10"); }
    @Test void healthyAndLowBoundary() { assertThat(UsageCalculator.calculate(product(50,10),now.minus(Duration.ofDays(8)),now,new BigDecimal("28"),2).status()).isEqualTo("HEALTHY");assertThat(UsageCalculator.calculate(product(10,10),now.minus(Duration.ofDays(8)),now,new BigDecimal("28"),2).status()).isEqualTo("LOW_STOCK"); }
    @Test void partialObservedDaysAreIncluded() { var i=UsageCalculator.calculate(product(12,10),now.minus(Duration.ofHours(60)),now,new BigDecimal("10"),2);assertThat(i.observedDays()).isEqualByComparingTo("2.5");assertThat(i.averageDailyUsage()).isEqualByComparingTo("4"); }
}

