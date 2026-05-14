package com.yourapp.recurring;

import com.yourapp.transaction.Transaction;
import com.yourapp.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringScheduler {

    private final RecurringRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void generateTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringRule> dueRules = ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(today);

        log.info("Recurring scheduler: processing {} due rules", dueRules.size());

        for (RecurringRule rule : dueRules) {
            if (rule.getEndDate() != null && today.isAfter(rule.getEndDate())) {
                rule.setActive(false);
                ruleRepository.save(rule);
                continue;
            }

            Transaction transaction = Transaction.builder()
                    .user(rule.getUser())
                    .type(rule.getType())
                    .amount(rule.getAmount())
                    .currency(rule.getCurrency())
                    .exchangeRate(rule.getExchangeRate())
                    .fromAccount(rule.getFromAccount())
                    .toAccount(rule.getToAccount())
                    .category(rule.getCategory())
                    .transactionDate(rule.getNextOccurrence().atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                    .recurringRuleId(rule.getId())
                    .tags(new HashSet<>())
                    .build();

            transactionRepository.save(transaction);

            LocalDate next = advanceDate(rule.getNextOccurrence(), rule.getFrequency(), rule.getIntervalValue());
            rule.setNextOccurrence(next);

            if (rule.getEndDate() != null && next.isAfter(rule.getEndDate())) {
                rule.setActive(false);
            }

            ruleRepository.save(rule);
        }
    }

    private LocalDate advanceDate(LocalDate date, String frequency, int interval) {
        return switch (frequency) {
            case "DAILY" -> date.plusDays(interval);
            case "WEEKLY" -> date.plusWeeks(interval);
            case "MONTHLY" -> date.plusMonths(interval);
            case "YEARLY" -> date.plusYears(interval);
            default -> throw new IllegalArgumentException("Unknown frequency: " + frequency);
        };
    }
}
