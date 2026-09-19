package com.voicestock.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.voicestock.dto.ApiModels.*;
import com.voicestock.service.AuthService;

@RestController @RequestMapping("/api") @Profile("postgres")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }
    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @GetMapping("/users/profile")
    public UserView profile(@AuthenticationPrincipal Jwt jwt) { return auth.profile(UUID.fromString(jwt.getSubject())); }
    @PutMapping("/users/profile")
    public UserView update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest request) { return auth.update(UUID.fromString(jwt.getSubject()),request); }
}
