package com.yourapp.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.category.id = :categoryId
              AND t.type = 'EXPENSE'
              AND t.transactionDate >= :periodStart
              AND t.transactionDate < :periodEnd
            """)
    BigDecimal computeSpent(@Param("userId") UUID userId,
                            @Param("categoryId") UUID categoryId,
                            @Param("periodStart") OffsetDateTime periodStart,
                            @Param("periodEnd") OffsetDateTime periodEnd);
}
