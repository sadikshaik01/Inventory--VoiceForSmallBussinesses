package com.voicestock.repository;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import com.voicestock.entity.BusinessAction;

public interface BusinessActionRepository extends JpaRepository<BusinessAction,UUID> {
    List<BusinessAction> findByUserIdOrderByCreatedAtDesc(UUID userId,Pageable page);
    List<BusinessAction> findByUserIdAndStatusOrderByApprovedAtDesc(UUID userId,String status);
    Optional<BusinessAction> findByUserIdAndProductIdAndStatus(UUID userId,UUID productId,String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BusinessAction a where a.id=:id and a.user.id=:owner")
    Optional<BusinessAction> locked(@Param("owner") UUID owner,@Param("id") UUID id);
}
