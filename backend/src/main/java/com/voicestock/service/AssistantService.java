package com.voicestock.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.voicestock.dto.AssistantModels.*;
import com.voicestock.dto.ApiModels.*;
import com.voicestock.entity.*;
import com.voicestock.exception.ApiException;

@Service @Profile("postgres")
public class AssistantService {
    private final InventoryService inventory;
    private final CommandParser parser;
    private final BusinessService business;
    private final jakarta.validation.Validator validator;
    private final Map<UUID,Draft> drafts=new ConcurrentHashMap<>();
    private static final class Draft {
        final UUID owner; final Command command; final ProductView product;
        final Instant expires=Instant.now().plusSeconds(600); ProductView result;
        Draft(UUID owner,Command command,ProductView product) { this.owner=owner;this.command=command;this.product=product; }
    }
    public AssistantService(InventoryService inventory,CommandParser parser,BusinessService business,jakarta.validation.Validator validator) { this.inventory=inventory;this.parser=parser;this.business=business;this.validator=validator; }
    public Reply interpret(UUID owner,String text) { return preview(owner,parser.parse(text)); }
    private Reply message(Command c,String message) { return new Reply(c,message,null,null,null,List.of()); }
    private Command missing(Command c,List<String> missing) { return new Command(c.intent(),c.product(),c.quantity(),c.unit(),c.price(),c.language(),c.confidence(),missing,c.productId()); }
    private List<ProductView> matches(UUID owner,Command c) {
        var catalog=inventory.catalog(owner);
        if(c.productId()!=null) return catalog.stream().filter(p->p.id().equals(c.productId())).toList();
        String name=CommandParser.normalized(c.product());
        var exact=catalog.stream().filter(p->CommandParser.normalized(p.name()).equals(name)).toList();
        if(!exact.isEmpty()) return exact;
        return catalog.stream().filter(p->(" "+CommandParser.normalized(p.name())+" ").contains(" "+name+" ")).toList();
    }
    public Reply preview(UUID owner,Command input) {
        Command c=input;
        if(!validator.validate(c).isEmpty()) return message(c,"The command contains an invalid value. Check the product, quantity, unit and price.");
        if(Set.of(Intent.PENDING_ACTIONS,Intent.APPROVED_REORDERS).contains(c.intent())) {
            var actions=c.intent()==Intent.PENDING_ACTIONS?business.pending(owner):business.history(owner).stream().filter(a->a.approvedAt()!=null).toList();
            return new Reply(c,actions.isEmpty()?"No approved reorder actions match this question.":"Your saved reorder actions. Approving or completing a task does not change stock.",null,null,null,List.of(),List.of(),List.of(),actions);
        }
        if(Set.of(Intent.ATTENTION,Intent.RUNNING_OUT,Intent.REORDER).contains(c.intent())) {
            boolean soon=c.intent()==Intent.RUNNING_OUT;
            var insights=business.insights(owner).stream().filter(i->!i.status().equals("HEALTHY") && (!soon || (i.daysRemaining()!=null && i.daysRemaining().compareTo(new BigDecimal("3"))<=0))).toList();
            // Keep the existing reorder product response for API compatibility.
            return new Reply(c,insights.isEmpty()?"No products match this question.":"Here is your current inventory from the database.",null,null,null,insights.stream().map(i->i.product()).toList(),List.of(),insights,List.of());
        }
        if(Set.of(Intent.LOW_STOCK,Intent.OUT_OF_STOCK).contains(c.intent())) {
            List<ProductView> found=new ArrayList<>(); int page=0; PageView<ProductView> batch;
            do { batch=inventory.list(owner,"",true,page++,100); for(var p:batch.items()) if(c.intent()==Intent.OUT_OF_STOCK?p.currentStock().signum()==0:p.currentStock().signum()>0) found.add(p); } while(page<batch.totalPages());
            return new Reply(c,found.isEmpty()?"No products match this question.":"Here is your current inventory from the database.",null,null,null,found);
        }
        if((c.product()==null || c.product().isBlank()) && c.productId()==null) return message(missing(c,List.of("product")),"Please specify the product name and intended action. Nothing has been changed.");
        var found=matches(owner,c);
        if(found.isEmpty()) return message(missing(c,List.of("product")),"I couldn't find that product in your inventory.");
        if(found.size()!=1) return new Reply(missing(c,List.of("product")),"Several products match. Select the product you mean.",null,null,null,List.of(),found,List.of(),List.of());
        var p=found.getFirst(); c=c.resolved(p);
        if(c.intent()==Intent.CHECK_STOCK) return new Reply(c,"You have "+number(p.currentStock())+" "+p.unit().getValue()+" of "+p.name()+".",null,p,null,List.of());
        if(Set.of(Intent.STOCK_DURATION,Intent.REORDER_REASON).contains(c.intent())) {
            var insight=business.insights(owner).stream().filter(i->i.product().id().equals(p.id())).findFirst().orElseThrow();
            return new Reply(c,insight.reason(),null,p,null,List.of(),List.of(),List.of(insight),List.of());
        }
        List<String> absent=new ArrayList<>();
        if(c.intent()==Intent.UNKNOWN) absent.add("intent");
        if(c.quantity()==null) absent.add("quantity");
        if(c.unit()==null || c.unit().isBlank()) absent.add("unit");
        c=missing(c,absent);
        if(!absent.isEmpty()) return new Reply(c,c.intent()==Intent.UNKNOWN?"What would you like to do? Choose Add stock or Remove stock; supply any missing quantity and unit.":"I understood that you want to update "+p.name()+", but I need the quantity and unit.",null,p,null,List.of());
        if(c.quantity().signum()<=0) return message(c,"Quantity must be greater than zero, with at most 3 decimal places.");
        String normalized=CommandParser.unit(c.unit());
        if(!p.unit().name().equals(normalized)) return message(c,"Use this product's unit: "+p.unit().getValue()+". Units cannot be converted automatically.");
        var after=c.intent()==Intent.ADD_STOCK?p.currentStock().add(c.quantity()):p.currentStock().subtract(c.quantity());
        if(after.signum()<0) return message(c,"Only "+number(p.currentStock())+" "+p.unit().getValue()+" available. You cannot remove more than this.");
        if(after.compareTo(new BigDecimal("9999999999999999.999"))>0) return message(c,"This quantity exceeds the supported stock limit.");
        drafts.entrySet().removeIf(e->e.getValue().expires.isBefore(Instant.now()));
        if(drafts.size()>=1000) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Too many pending commands. Please try again in a few minutes.");
        UUID id=UUID.randomUUID(); drafts.put(id,new Draft(owner,c,p));
        return new Reply(c,"Review the change, then confirm. Nothing has been changed yet.",id,p,after,List.of());
    }
    private Draft draft(UUID owner,UUID id) {
        var d=drafts.get(id);
        if(d==null || !d.owner.equals(owner) || d.expires.isBefore(Instant.now())) throw new ApiException(HttpStatus.NOT_FOUND,"This confirmation expired. Process the command again.");
        return d;
    }
    public ProductView confirm(UUID owner,UUID id) {
        var d=draft(owner,id);
        synchronized(d) {
            if(drafts.get(id)!=d) throw new ApiException(HttpStatus.CONFLICT,"This command was cancelled.");
            if(d.result==null) d.result=inventory.changeStock(owner,new StockRequest(d.product.id(),d.command.quantity(),d.product.unit()),d.command.intent()==Intent.ADD_STOCK?TransactionType.ADD:TransactionType.REMOVE,TransactionSource.VOICE,d.product.currentStock());
            return d.result;
        }
    }
    public void cancel(UUID owner,UUID id) { var d=drafts.get(id);if(d!=null && d.owner.equals(owner)) synchronized(d) { drafts.remove(id,d); } }
    private String number(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
}
