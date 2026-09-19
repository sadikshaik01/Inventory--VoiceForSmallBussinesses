package com.voicestock.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.voicestock.dto.ApiModels.*;
import com.voicestock.entity.*;
import com.voicestock.exception.ApiException;
import com.voicestock.repository.*;

@Service @Profile("postgres") @Transactional(readOnly=true)
public class InventoryService {
    private final UserRepository users;
    private final ProductRepository products;
    private final InventoryTransactionRepository transactions;
    public InventoryService(UserRepository users, ProductRepository products, InventoryTransactionRepository transactions) {
        this.users=users; this.products=products; this.transactions=transactions;
    }
    private Pageable page(int page, int size, Sort sort) {
        if(page<0 || size<1 || size>100) throw new ApiException(HttpStatus.BAD_REQUEST,"Page must be positive and page size must be between 1 and 100.");
        return PageRequest.of(page,size,sort);
    }
    public PageView<ProductView> list(UUID owner, String search, boolean alerts, int page, int size) {
        String escaped=search.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
        if(escaped.length()>320) throw new ApiException(HttpStatus.BAD_REQUEST,"Search is too long.");
        Specification<Product> filter=(root,query,cb) -> {
            var base=cb.and(cb.equal(root.get("user").get("id"),owner),cb.isFalse(root.get("archived")));
            if(!escaped.isBlank()) base=cb.and(base,cb.or(cb.like(cb.lower(root.get("name")),"%"+escaped+"%",'\\'), cb.like(cb.lower(root.get("category")),"%"+escaped+"%",'\\')));
            if(alerts) base=cb.and(base,cb.lessThanOrEqualTo(root.get("currentStock"),root.get("minimumStock")));
            return base;
        };
        return PageView.of(products.findAll(filter,page(page,size,Sort.by("name","id"))).map(ProductView::of));
    }
    public java.util.List<ProductView> catalog(UUID owner) { return products.findAllByUserIdAndArchivedFalse(owner).stream().map(ProductView::of).toList(); }
    public ProductView details(UUID owner, UUID id) { return ProductView.of(products.findByIdAndUserId(id,owner).orElseThrow(this::missing)); }
    public java.util.List<ProductView> named(UUID owner,String name) {
        return products.findTop2ByUserIdAndArchivedFalseAndNameIgnoreCase(owner,name.trim()).stream().map(ProductView::of).toList();
    }
    private ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"Product not found."); }
    private Product locked(UUID owner, UUID id) { return products.lockActive(id,owner).orElseThrow(this::missing); }

    @Transactional
    public ProductView create(UUID owner, ProductCreate request) {
        var user=users.findById(owner).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Please sign in again."));
        var product=products.saveAndFlush(new Product(user,request.name().trim(),request.category().trim(),request.unit(),request.currentStock(),request.minimumStock(),request.price()));
        // Opening stock is an ADD entry so the transaction ledger matches current stock.
        if(request.currentStock().signum()>0) transactions.save(new InventoryTransaction(user,product,TransactionType.ADD,request.currentStock(),request.unit(),TransactionSource.MANUAL));
        return ProductView.of(product);
    }
    @Transactional
    public ProductView update(UUID owner, UUID id, ProductEdit request) {
        var product=locked(owner,id);
        if(product.getUnit()!=request.unit() && (product.getCurrentStock().signum()!=0 || transactions.existsByProductId(id)))
            throw new ApiException(HttpStatus.BAD_REQUEST,"A unit cannot change after stock has been recorded. Create a separate product for the new unit.");
        product.updateDetails(request.name().trim(),request.category().trim(),request.unit(),request.minimumStock(),request.price());
        products.flush();
        return ProductView.of(product);
    }
    @Transactional
    public void archive(UUID owner, UUID id) { locked(owner,id).archive(); }

    @Transactional
    public ProductView changeStock(UUID owner, StockRequest request, TransactionType type, TransactionSource source) {
        return changeStock(owner,request,type,source,null);
    }
    @Transactional
    public ProductView changeStock(UUID owner, StockRequest request, TransactionType type, TransactionSource source, BigDecimal expectedStock) {
        var product=locked(owner,request.productId());
        if(expectedStock!=null && product.getCurrentStock().compareTo(expectedStock)!=0)
            throw new ApiException(HttpStatus.CONFLICT,"Stock changed since preview. Process the command again to review current stock.");
        if(product.getUnit()!=request.unit()) throw new ApiException(HttpStatus.BAD_REQUEST,"Use this product's unit: "+product.getUnit().getValue()+".");
        if(request.quantity()==null || request.quantity().signum()<=0) throw new ApiException(HttpStatus.BAD_REQUEST,"Quantity must be greater than zero.");
        BigDecimal next=type==TransactionType.ADD ? product.getCurrentStock().add(request.quantity()) : product.getCurrentStock().subtract(request.quantity());
        if(next.signum()<0) throw new ApiException(HttpStatus.BAD_REQUEST,"Only "+product.getCurrentStock().stripTrailingZeros().toPlainString()+" "+product.getUnit().getValue()+" available. You cannot remove more than this.");
        if(next.compareTo(new BigDecimal("9999999999999999.999"))>0) throw new ApiException(HttpStatus.BAD_REQUEST,"This quantity exceeds the supported stock limit.");
        product.setCurrentStock(next);
        transactions.save(new InventoryTransaction(product.getUser(),product,type,request.quantity(),request.unit(),source));
        products.flush();
        return ProductView.of(product);
    }
    public PageView<TransactionView> history(UUID owner, UUID productId, int page, int size) {
        var paging=page(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"));
        var result=productId==null ? transactions.findAllByUserIdOrderByCreatedAtDesc(owner,paging)
            : transactions.findAllByUserIdAndProductIdOrderByCreatedAtDesc(owner,productId,paging);
        return PageView.of(result.map(TransactionView::of));
    }
    public DashboardView dashboard(UUID owner, String timeZone) {
        ZoneId zone;
        try { zone=ZoneId.of(timeZone); } catch (DateTimeException ex) { throw new ApiException(HttpStatus.BAD_REQUEST,"Invalid time zone."); }
        var today=LocalDate.now(zone);
        long count=transactions.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(owner,today.atStartOfDay(zone).toInstant(),today.plusDays(1).atStartOfDay(zone).toInstant());
        return new DashboardView(products.countByUserIdAndArchivedFalse(owner),products.countLowStock(owner),products.countOutOfStock(owner),count,history(owner,null,0,5).items());
    }
}

