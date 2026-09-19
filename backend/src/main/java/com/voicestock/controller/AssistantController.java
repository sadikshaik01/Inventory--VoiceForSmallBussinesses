package com.voicestock.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.voicestock.dto.AssistantModels.*;
import com.voicestock.dto.ApiModels.ProductView;
import com.voicestock.service.AssistantService;

@RestController @RequestMapping("/api/assistant") @Profile("postgres")
public class AssistantController {
    private final AssistantService assistant;
    public AssistantController(AssistantService assistant) { this.assistant=assistant; }
    @PostMapping("/interpret") public Reply interpret(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Input input) { return assistant.interpret(UUID.fromString(jwt.getSubject()),input.text()); }
    @PostMapping("/preview") public Reply preview(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Command command) { return assistant.preview(UUID.fromString(jwt.getSubject()),command); }
    @PostMapping("/confirm/{id}") public ProductView confirm(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return assistant.confirm(UUID.fromString(jwt.getSubject()),id); }
    @DeleteMapping("/confirm/{id}") public void cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { assistant.cancel(UUID.fromString(jwt.getSubject()),id); }
}
