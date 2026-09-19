package com.voicestock.controller;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.voicestock.service.BusinessService;
import com.voicestock.dto.BusinessModels.*;

@RestController @RequestMapping("/api/business") @Profile("postgres")
public class BusinessController {
    private final BusinessService business;
    public BusinessController(BusinessService business) { this.business=business; }
    @GetMapping public Center center(@AuthenticationPrincipal Jwt jwt) { return business.center(UUID.fromString(jwt.getSubject())); }
    @PostMapping("/approve/{productId}") public ActionView approve(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID productId) { return business.approve(UUID.fromString(jwt.getSubject()),productId); }
    @PostMapping("/actions/{id}/complete") public ActionView complete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return business.transition(UUID.fromString(jwt.getSubject()),id,true); }
    @PostMapping("/actions/{id}/cancel") public ActionView cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return business.transition(UUID.fromString(jwt.getSubject()),id,false); }
}
