package com.voicestock.database;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.voicestock.entity.*;
import com.voicestock.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("postgres")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
@Transactional
class RepositoryIT {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TestDatabase.JWT_SECRET);
        registry.add("spring.datasource.url", () -> TestDatabase.required("TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> TestDatabase.required("TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> TestDatabase.required("TEST_DB_PASSWORD"));
    }

    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired InventoryTransactionRepository transactions;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    User owner;
    Product product;

    @BeforeEach
    void fixture() {
        owner = users.saveAndFlush(TestDatabase.user());
        product = products.saveAndFlush(new Product(owner, "Test rice", "Grocery", InventoryUnit.KG,
                new BigDecimal("12.375"), new BigDecimal("3.500"), new BigDecimal("49.95")));
    }

    private InventoryTransaction transaction() {
        return transactions.saveAndFlush(new InventoryTransaction(owner, product, TransactionType.ADD,
                new BigDecimal("12.375"), InventoryUnit.KG, TransactionSource.MANUAL));
    }

    @Test
    void roundTripsEntitiesDecimalUnitsAndTimestamps() {
        UUID transactionId = transaction().getId();
        entityManager.clear();
        var stored = products.findByIdAndUserId(product.getId(), owner.getId()).orElseThrow();
        assertThat(stored.getCurrentStock()).isEqualByComparingTo("12.375");
        assertThat(stored.getMinimumStock()).isEqualByComparingTo("3.500");
        assertThat(stored.getPrice()).isEqualByComparingTo("49.95");
        assertThat(stored.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(stored.getUpdatedAt()).isNotNull();
        assertThat(transactions.findByIdAndUserId(transactionId, owner.getId()).orElseThrow().getUnit()).isEqualTo(InventoryUnit.KG);
        assertThat(users.findByEmailIgnoreCase(owner.getEmail().toUpperCase())).isPresent();
    }

    @Test
    void userScopedRepositoriesDoNotReturnAnotherOwnersData() {
        var other = users.saveAndFlush(TestDatabase.user());
        var entry = transaction();
        assertThat(products.findByIdAndUserId(product.getId(), other.getId())).isEmpty();
        assertThat(products.findAllByUserId(other.getId(), PageRequest.of(0, 10))).isEmpty();
        assertThat(transactions.findByIdAndUserId(entry.getId(), other.getId())).isEmpty();
        assertThat(transactions.findAllByUserIdOrderByCreatedAtDesc(other.getId(), PageRequest.of(0, 10))).isEmpty();
        assertThat(transactions.findAllByUserIdAndProductIdOrderByCreatedAtDesc(owner.getId(), product.getId(), PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void databaseRejectsCrossOwnerTransaction() {
        var other = users.saveAndFlush(TestDatabase.user());
        var entry = transaction();
        assertThatThrownBy(() -> jdbc.update("UPDATE inventory_transactions SET user_id=? WHERE id=?", other.getId(), entry.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsNegativeStock() {
        assertThatThrownBy(() -> jdbc.update("UPDATE products SET current_stock=-1 WHERE id=?", product.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsCaseInsensitiveDuplicateEmail() {
        var other = users.saveAndFlush(TestDatabase.user());
        assertThatThrownBy(() -> jdbc.update("UPDATE users SET email=? WHERE id=?", owner.getEmail().toUpperCase(), other.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void historicalTransactionsPreventProductDeletion() {
        transaction();
        assertThatThrownBy(() -> jdbc.update("DELETE FROM products WHERE id=?", product.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatesMaintainAuditTimestampAndPriceCanBeAbsent() {
        var original = product.getUpdatedAt();
        product.setName("Updated test rice");
        products.saveAndFlush(product);
        entityManager.clear();
        assertThat(products.findById(product.getId()).orElseThrow().getUpdatedAt()).isAfter(original);
        var noPrice = products.saveAndFlush(new Product(owner, "Test boxes", "General", InventoryUnit.BOXES,
                BigDecimal.ZERO, BigDecimal.ZERO, null));
        assertThat(noPrice.getPrice()).isNull();
    }
}
