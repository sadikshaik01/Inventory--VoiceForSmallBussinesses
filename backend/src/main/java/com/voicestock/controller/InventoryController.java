package com.voicestock.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.voicestock.dto.ApiModels.*;
import com.voicestock.entity.*;
import com.voicestock.service.InventoryService;

@RestController @RequestMapping("/api") @Profile("postgres")
public class InventoryController {
    private final InventoryService inventory;
    public InventoryController(InventoryService inventory) { this.inventory=inventory; }
    private UUID owner(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @GetMapping({"/products","/inventory"})
    public PageView<ProductView> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return inventory.list(owner(jwt),search,false,page,size);
    }
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ProductCreate request) { return inventory.create(owner(jwt),request); }
    @GetMapping("/products/{id}")
    public ProductView details(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return inventory.details(owner(jwt),id); }
    @PutMapping("/products/{id}")
    public ProductView update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ProductEdit request) { return inventory.update(owner(jwt),id,request); }
    @DeleteMapping("/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { inventory.archive(owner(jwt),id); }
    @PostMapping("/inventory/add")
    public ProductView add(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody StockRequest request) { return inventory.changeStock(owner(jwt),request,TransactionType.ADD,TransactionSource.MANUAL); }
    @PostMapping("/inventory/remove")
    public ProductView remove(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody StockRequest request) { return inventory.changeStock(owner(jwt),request,TransactionType.REMOVE,TransactionSource.MANUAL); }
    @GetMapping("/transactions")
    public PageView<TransactionView> history(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID productId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) { return inventory.history(owner(jwt),productId,page,size); }
    @GetMapping("/inventory/low-stock")
    public PageView<ProductView> alerts(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return inventory.list(owner(jwt),"",true,page,size);
    }
    @GetMapping("/dashboard")
    public DashboardView dashboard(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="UTC") String timeZone) { return inventory.dashboard(owner(jwt),timeZone); }
}
