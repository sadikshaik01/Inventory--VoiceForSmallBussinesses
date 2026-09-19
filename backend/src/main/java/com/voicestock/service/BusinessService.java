package com.voicestock.service;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import com.voicestock.entity.*;
import com.voicestock.repository.*;
import com.voicestock.dto.ApiModels.ProductView;
import com.voicestock.dto.BusinessModels.*;
import com.voicestock.exception.ApiException;

@Service @Profile("postgres") @Transactional(readOnly=true)
public class BusinessService {
    private final ProductRepository products;
    private final BusinessActionRepository actions;
    private final JdbcTemplate jdbc;
    public BusinessService(ProductRepository products,BusinessActionRepository actions,JdbcTemplate jdbc) { this.products=products;this.actions=actions;this.jdbc=jdbc; }
    private record Usage(BigDecimal removed,int dates) {}
    public List<Insight> insights(UUID owner) {
        Instant now=Instant.now();
        Map<UUID,Usage> usage=new HashMap<>();
        jdbc.query("SELECT product_id,sum(quantity) removed,count(distinct (created_at AT TIME ZONE 'UTC')::date) dates FROM inventory_transactions WHERE user_id=? AND transaction_type='REMOVE' AND created_at>=? AND created_at<=? GROUP BY product_id",
            (org.springframework.jdbc.core.RowCallbackHandler) rs->usage.put(rs.getObject("product_id",UUID.class),new Usage(rs.getBigDecimal("removed"),rs.getInt("dates"))),owner,Timestamp.from(now.minus(Duration.ofDays(7))),Timestamp.from(now));
        return products.findAllByUserIdAndArchivedFalse(owner).stream().map(p->{ var u=usage.getOrDefault(p.getId(),new Usage(BigDecimal.ZERO,0));return UsageCalculator.calculate(ProductView.of(p),p.getCreatedAt(),now,u.removed(),u.dates()); }).sorted(Comparator.comparing(i->i.product().name())).toList();
    }
    public List<ActionView> history(UUID owner) { return actions.findByUserIdOrderByCreatedAtDesc(owner,PageRequest.of(0,100)).stream().map(ActionView::of).toList(); }
    public List<ActionView> pending(UUID owner) { return actions.findByUserIdAndStatusOrderByApprovedAtDesc(owner,"APPROVED").stream().map(ActionView::of).toList(); }
    public Center center(UUID owner) {
        var attention=insights(owner).stream().filter(i->!i.status().equals("HEALTHY")).toList();
        return new Center(attention,history(owner),attention.stream().filter(i->i.status().equals("LOW_STOCK")).count(),attention.stream().filter(i->i.status().equals("OUT_OF_STOCK")).count(),attention.stream().filter(i->i.status().equals("RUNNING_OUT_SOON")).count());
    }
    @Transactional public ActionView approve(UUID owner,UUID productId) {
        var p=products.lockActive(productId,owner).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Product not found."));
        var existing=actions.findByUserIdAndProductIdAndStatus(owner,productId,"APPROVED");
        if(existing.isPresent()) return ActionView.of(existing.get());
        var insight=insights(owner).stream().filter(i->i.product().id().equals(productId)).findFirst().orElseThrow();
        if(insight.status().equals("HEALTHY") || insight.suggestedReorderQuantity().signum()<=0) throw new ApiException(HttpStatus.CONFLICT,"This product no longer needs a reorder. Refresh the Action Center.");
        return ActionView.of(actions.saveAndFlush(new BusinessAction(p,insight.suggestedReorderQuantity(),insight.reason())));
    }
    @Transactional public ActionView transition(UUID owner,UUID id,boolean complete) {
        var a=actions.locked(owner,id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Action not found."));
        String target=complete?"COMPLETED":"CANCELLED";
        if(a.getStatus().equals(target)) return ActionView.of(a);
        if(!a.getStatus().equals("APPROVED")) throw new ApiException(HttpStatus.CONFLICT,"Only approved actions can be completed or cancelled.");
        if(complete) a.complete(); else a.cancel();
        actions.flush();return ActionView.of(a);
    }
}
