package com.voicestock.entity;

import java.util.Locale;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "users")
public class User extends AuditedEntity {
    @NotBlank @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank @Email @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String email;

    // This field stores only a BCrypt password hash; never expose it in API responses.
    @JsonIgnore
    @NotBlank @Size(max = 255)
    @Column(name = "password", nullable = false, length = 255)
    private String passwordHash;

    @NotBlank @Size(max = 160)
    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    @NotBlank @Size(max = 20)
    @Column(name = "preferred_language", nullable = false, length = 20)
    private String preferredLanguage;

    protected User() {}

    public User(String name, String email, String passwordHash, String businessName, String preferredLanguage) {
        this.name = name;
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.businessName = businessName;
        this.preferredLanguage = preferredLanguage;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    @JsonIgnore public String getPasswordHash() { return passwordHash; }
    public String getBusinessName() { return businessName; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setName(String name) { this.name = name; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
}
