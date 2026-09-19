package com.voicestock.service;
import java.math.*;
import java.time.*;
import com.voicestock.dto.ApiModels.ProductView;
import com.voicestock.dto.BusinessModels.Insight;

public final class UsageCalculator {
    private UsageCalculator() {}
    public static Insight calculate(ProductView p,Instant created,Instant now,BigDecimal removed,int removalDates) {
        // Keep partial observed days in the denominator; require two full days and two removal dates.
        BigDecimal days=BigDecimal.valueOf(Math.max(0,Math.min(604800,Duration.between(created,now).getSeconds()))).divide(BigDecimal.valueOf(86400),6,RoundingMode.HALF_UP);
        BigDecimal average=days.compareTo(BigDecimal.valueOf(2))>=0 && removalDates>=2 && removed.signum()>0 ? removed.divide(days,6,RoundingMode.HALF_UP) : null;
        BigDecimal remaining=average!=null && average.signum()>0 ? p.currentStock().divide(average,1,RoundingMode.HALF_UP) : null;
        boolean soon=average!=null && p.currentStock().compareTo(average.multiply(BigDecimal.valueOf(3)))<=0;
        String status=p.currentStock().signum()==0?"OUT_OF_STOCK":p.currentStock().compareTo(p.minimumStock())<=0?"LOW_STOCK":soon?"RUNNING_OUT_SOON":"HEALTHY";
        BigDecimal suggested=p.reorderQuantity();
        if(status.equals("RUNNING_OUT_SOON")) {
            BigDecimal target=p.minimumStock().multiply(BigDecimal.valueOf(2)).max(p.minimumStock().add(BigDecimal.ONE)).max(average.multiply(BigDecimal.valueOf(7)));
            suggested=target.subtract(p.currentStock()).max(BigDecimal.ZERO).setScale(3,RoundingMode.CEILING);
        }
        suggested=suggested.min(new BigDecimal("9999999999999999.999"));
        String reason=p.name()+" has "+n(p.currentStock())+" "+p.unit().getValue()+" remaining; its minimum is "+n(p.minimumStock())+" "+p.unit().getValue()+". ";
        reason+=average==null?"Not enough recent usage data to estimate run-out time.":"Based on "+n(removed)+" "+p.unit().getValue()+" removed over "+n(days)+" observed days, recent usage averages "+n(average)+" per day. At that recorded rate, stock may last approximately "+n(remaining)+" days. This is an estimate, not a forecast of sales.";
        return new Insight(p,status,removed,days,average,remaining,suggested,reason);
    }
    private static String n(BigDecimal n) { return n.stripTrailingZeros().toPlainString(); }
}

