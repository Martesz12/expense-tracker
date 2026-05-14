package com.yourapp.recurring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringRuleRepository extends JpaRepository<RecurringRule, UUID> {

    List<RecurringRule> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<RecurringRule> findByIdAndUserId(UUID id, UUID userId);

    List<RecurringRule> findByActiveTrueAndNextOccurrenceLessThanEqual(LocalDate date);
}
