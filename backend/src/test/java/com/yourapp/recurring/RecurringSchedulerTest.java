package com.yourapp.recurring;

import com.yourapp.account.Account;
import com.yourapp.transaction.Transaction;
import com.yourapp.transaction.TransactionRepository;
import com.yourapp.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringSchedulerTest {

    @Mock RecurringRuleRepository ruleRepository;
    @Mock TransactionRepository transactionRepository;

    @InjectMocks RecurringScheduler scheduler;

    private User buildUser() {
        return User.builder().id(UUID.randomUUID()).name("Alice").email("alice@example.com").password("pw").build();
    }

    private Account buildAccount() {
        return Account.builder().id(UUID.randomUUID()).user(buildUser())
                .name("Checking").type("CHECKING").currency("EUR").initialBalance(BigDecimal.ZERO).build();
    }

    private RecurringRule buildRule(LocalDate nextOccurrence, LocalDate endDate, boolean active) {
        return RecurringRule.builder()
                .id(UUID.randomUUID())
                .user(buildUser())
                .type("EXPENSE")
                .amount(new BigDecimal("50.00"))
                .currency("EUR")
                .fromAccount(buildAccount())
                .frequency("MONTHLY")
                .intervalValue(1)
                .startDate(nextOccurrence)
                .endDate(endDate)
                .nextOccurrence(nextOccurrence)
                .active(active)
                .build();
    }

    @Test
    void generateTransactions_noDueRules_doesNothing() {
        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(any())).thenReturn(List.of());

        scheduler.generateTransactions();

        verify(transactionRepository, never()).save(any());
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void generateTransactions_dueRule_createsTransactionAndAdvancesDate() {
        LocalDate today = LocalDate.now();
        RecurringRule rule = buildRule(today, null, true);

        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(today)).thenReturn(List.of(rule));

        scheduler.generateTransactions();

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo("50.00");

        ArgumentCaptor<RecurringRule> ruleCaptor = ArgumentCaptor.forClass(RecurringRule.class);
        verify(ruleRepository).save(ruleCaptor.capture());
        assertThat(ruleCaptor.getValue().getNextOccurrence()).isEqualTo(today.plusMonths(1));
    }

    @Test
    void generateTransactions_ruleAlreadyPastEndDate_deactivatesWithoutCreatingTransaction() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().minusDays(2);
        RecurringRule rule = buildRule(yesterday, endDate, true);

        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(LocalDate.now())).thenReturn(List.of(rule));

        scheduler.generateTransactions();

        verify(transactionRepository, never()).save(any());
        assertThat(rule.isActive()).isFalse();
        verify(ruleRepository).save(rule);
    }

    @Test
    void generateTransactions_nextOccurrencePastEndDate_deactivatesAfterCreating() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(10);
        RecurringRule rule = buildRule(today, endDate, true);
        rule.setIntervalValue(100);

        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(today)).thenReturn(List.of(rule));

        scheduler.generateTransactions();

        verify(transactionRepository).save(any());
        assertThat(rule.isActive()).isFalse();
    }

    @Test
    void advanceDate_daily_addsDays() {
        LocalDate today = LocalDate.now();
        RecurringRule rule = buildRule(today, null, true);
        rule.setFrequency("DAILY");
        rule.setIntervalValue(3);

        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(today)).thenReturn(List.of(rule));

        scheduler.generateTransactions();

        assertThat(rule.getNextOccurrence()).isEqualTo(today.plusDays(3));
    }

    @Test
    void advanceDate_weekly_addsWeeks() {
        LocalDate today = LocalDate.now();
        RecurringRule rule = buildRule(today, null, true);
        rule.setFrequency("WEEKLY");
        rule.setIntervalValue(2);

        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(today)).thenReturn(List.of(rule));

        scheduler.generateTransactions();

        assertThat(rule.getNextOccurrence()).isEqualTo(today.plusWeeks(2));
    }

    @Test
    void advanceDate_monthly_addsMonths() {
        LocalDate today = LocalDate.now();
        RecurringRule rule = buildRule(today, null, true);
        rule.setFrequency("MONTHLY");
        rule.setIntervalValue(1);

        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(today)).thenReturn(List.of(rule));

        scheduler.generateTransactions();

        assertThat(rule.getNextOccurrence()).isEqualTo(today.plusMonths(1));
    }

    @Test
    void advanceDate_yearly_addsYears() {
        LocalDate today = LocalDate.now();
        RecurringRule rule = buildRule(today, null, true);
        rule.setFrequency("YEARLY");
        rule.setIntervalValue(1);

        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(today)).thenReturn(List.of(rule));

        scheduler.generateTransactions();

        assertThat(rule.getNextOccurrence()).isEqualTo(today.plusYears(1));
    }

    @Test
    void advanceDate_unknownFrequency_throwsIllegalArgument() {
        LocalDate today = LocalDate.now();
        RecurringRule rule = buildRule(today, null, true);
        rule.setFrequency("HOURLY");

        when(ruleRepository.findByActiveTrueAndNextOccurrenceLessThanEqual(today)).thenReturn(List.of(rule));

        assertThatThrownBy(() -> scheduler.generateTransactions())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HOURLY");
    }
}
