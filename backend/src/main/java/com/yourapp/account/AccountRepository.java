package com.yourapp.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdOrderByNameAsc(UUID userId);

    List<Account> findByUserIdAndArchivedFalseOrderByNameAsc(UUID userId);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT a.initialBalance
              + COALESCE((SELECT SUM(t.amount) FROM Transaction t
                          WHERE t.fromAccount.id = :accountId AND t.user.id = :userId AND t.type = 'INCOME'), 0)
              - COALESCE((SELECT SUM(t.amount) FROM Transaction t
                          WHERE t.fromAccount.id = :accountId AND t.user.id = :userId AND t.type = 'EXPENSE'), 0)
              - COALESCE((SELECT SUM(t.amount) FROM Transaction t
                          WHERE t.fromAccount.id = :accountId AND t.user.id = :userId AND t.type = 'TRANSFER'), 0)
              + COALESCE((SELECT SUM(t.amount) FROM Transaction t
                          WHERE t.toAccount.id = :accountId AND t.user.id = :userId AND t.type = 'TRANSFER'), 0)
            FROM Account a
            WHERE a.id = :accountId AND a.user.id = :userId
            """)
    BigDecimal computeBalance(@Param("accountId") UUID accountId, @Param("userId") UUID userId);
}
