package com.voicestock.repository;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.voicestock.entity.InventoryTransaction;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    Optional<InventoryTransaction> findByIdAndUserId(UUID id, UUID userId);
    Page<InventoryTransaction> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<InventoryTransaction> findAllByUserIdAndProductIdOrderByCreatedAtDesc(UUID userId, UUID productId, Pageable pageable);
    boolean existsByProductId(UUID productId);
    long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(UUID userId, Instant start, Instant end);
}
