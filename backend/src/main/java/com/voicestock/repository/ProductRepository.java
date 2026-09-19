package com.voicestock.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.voicestock.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    java.util.List<Product> findAllByUserIdAndArchivedFalse(UUID userId);
    Optional<Product> findByIdAndUserId(UUID id, UUID userId);
    java.util.List<Product> findTop2ByUserIdAndArchivedFalseAndNameIgnoreCase(UUID userId,String name);
    Page<Product> findAllByUserId(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id=:id and p.user.id=:userId and p.archived=false")
    Optional<Product> lockActive(@Param("id") UUID id, @Param("userId") UUID userId);

    long countByUserIdAndArchivedFalse(UUID userId);

    @Query("select count(p) from Product p where p.user.id=:userId and p.archived=false and p.currentStock=0")
    long countOutOfStock(@Param("userId") UUID userId);

    @Query("select count(p) from Product p where p.user.id=:userId and p.archived=false and p.currentStock>0 and p.currentStock<=p.minimumStock")
    long countLowStock(@Param("userId") UUID userId);
}

