package com.voicestock.database;

import java.util.UUID;
import com.voicestock.entity.User;

final class TestDatabase {
    static final String JWT_SECRET = java.util.Base64.getEncoder().encodeToString(new java.security.SecureRandom().generateSeed(48));
    private TestDatabase() {}

    static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Set " + name + " for a separate PostgreSQL test database.");
        return value;
    }

    static String[] applicationArguments() {
        return new String[] {
            "--spring.config.import=", "--spring.profiles.active=postgres", "--server.port=0",
            "--app.jwt.secret=" + JWT_SECRET,
            "--spring.datasource.url=" + required("TEST_DB_URL"),
            "--spring.datasource.username=" + required("TEST_DB_USERNAME"),
            "--spring.datasource.password=" + required("TEST_DB_PASSWORD")
        };
    }

    static User user() {
        // Deliberately unusable credential: test fixtures must never become login accounts.
        return new User("Persistence test", UUID.randomUUID() + "@example.invalid",
                "!disabled-phase-2-fixture", "Test business", "en");
    }
}
