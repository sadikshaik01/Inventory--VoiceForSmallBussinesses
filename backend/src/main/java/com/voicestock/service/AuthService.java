package com.voicestock.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.voicestock.dto.ApiModels.*;
import com.voicestock.entity.User;
import com.voicestock.exception.ApiException;
import com.voicestock.repository.UserRepository;

@Service @Profile("postgres")
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final long lifetime;
    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder,
            @Value("${app.jwt.expiration-seconds}") long lifetime) {
        this.users=users; this.passwords=passwords; this.encoder=encoder;
        if (lifetime<300 || lifetime>86400) throw new IllegalStateException("JWT expiration must be between 300 and 86400 seconds.");
        this.lifetime=lifetime;
    }
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length>72) throw new ApiException(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes.");
        if (users.findByEmailIgnoreCase(request.email().trim()).isPresent()) throw new ApiException(HttpStatus.CONFLICT,"An account with this email already exists.");
        User user=users.saveAndFlush(new User(request.name().trim(),request.email(),passwords.encode(request.password()),request.businessName().trim(),request.preferredLanguage()));
        return session(user);
    }
    @Transactional(readOnly=true)
    public AuthResponse login(LoginRequest request) {
        User user=users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect."));
        if (request.password().getBytes(StandardCharsets.UTF_8).length>72 || !passwords.matches(request.password(),user.getPasswordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect.");
        return session(user);
    }
    private AuthResponse session(User user) {
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("voicestock").subject(user.getId().toString())
            .issuedAt(now).expiresAt(now.plusSeconds(lifetime)).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),claims)).getTokenValue();
        return new AuthResponse(token,UserView.of(user));
    }
    @Transactional(readOnly=true)
    public UserView profile(UUID id) { return UserView.of(user(id)); }
    @Transactional
    public UserView update(UUID id, ProfileRequest request) {
        var user=user(id); user.setName(request.name().trim()); user.setBusinessName(request.businessName().trim()); user.setPreferredLanguage(request.preferredLanguage());
        return UserView.of(users.save(user));
    }
    private User user(UUID id) { return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Please sign in again.")); }
}
