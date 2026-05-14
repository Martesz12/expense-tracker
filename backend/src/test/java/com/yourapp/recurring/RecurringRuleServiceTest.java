package com.yourapp.recurring;

import com.yourapp.account.Account;
import com.yourapp.account.AccountRepository;
import com.yourapp.category.Category;
import com.yourapp.category.CategoryRepository;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.recurring.dto.RecurringRuleRequest;
import com.yourapp.recurring.dto.RecurringRuleResponse;
import com.yourapp.user.User;
import com.yourapp.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringRuleServiceTest {

    @Mock RecurringRuleRepository ruleRepository;
    @Mock AccountRepository accountRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;

    @InjectMocks RecurringRuleService recurringRuleService;

    private final UUID userId = UUID.randomUUID();

    private User buildUser() {
        return User.builder().id(userId).name("Alice").email("alice@example.com").password("pw").build();
    }

    private Account buildAccount(UUID id, String name) {
        return Account.builder().id(id).user(buildUser()).name(name).type("CHECKING")
                .currency("EUR").initialBalance(BigDecimal.ZERO).build();
    }

    private Category buildCategory(UUID id) {
        return Category.builder().id(id).user(buildUser()).name("Food").type("EXPENSE").build();
    }

    private RecurringRuleRequest expenseRequest(UUID fromAccountId, UUID categoryId) {
        return new RecurringRuleRequest(
                "EXPENSE", new BigDecimal("50.00"), "EUR", null,
                fromAccountId, null, categoryId, "MONTHLY", 1,
                LocalDate.now(), null);
    }

    @Test
    void createRule_expense_success_setsNextOccurrenceAndActive() {
        UUID fromId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account fromAccount = buildAccount(fromId, "Checking");
        Category category = buildCategory(categoryId);
        category.setUser(buildUser());
        LocalDate startDate = LocalDate.now();

        RecurringRuleRequest req = expenseRequest(fromId, categoryId);
        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(Optional.of(fromAccount));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.getReferenceById(userId)).thenReturn(buildUser());
        when(ruleRepository.save(any(RecurringRule.class))).thenAnswer(inv -> {
            RecurringRule r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RecurringRuleResponse result = recurringRuleService.createRule(userId, req);

        assertThat(result.active()).isTrue();
        assertThat(result.nextOccurrence()).isEqualTo(startDate);
    }

    @Test
    void createRule_transfer_missingToAccount_throwsBadRequest400() {
        RecurringRuleRequest req = new RecurringRuleRequest(
                "TRANSFER", new BigDecimal("100.00"), "EUR", null,
                UUID.randomUUID(), null, null, "WEEKLY", 1, LocalDate.now(), null);

        assertThatThrownBy(() -> recurringRuleService.createRule(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createRule_income_missingCategory_throwsBadRequest400() {
        RecurringRuleRequest req = new RecurringRuleRequest(
                "INCOME", new BigDecimal("1000.00"), "EUR", null,
                UUID.randomUUID(), null, null, "MONTHLY", 1, LocalDate.now(), null);

        assertThatThrownBy(() -> recurringRuleService.createRule(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateRule_success_doesNotResetNextOccurrence() {
        UUID ruleId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account fromAccount = buildAccount(fromId, "Checking");
        Category category = buildCategory(categoryId);
        category.setUser(buildUser());
        LocalDate originalNext = LocalDate.now().plusDays(10);

        RecurringRule existing = RecurringRule.builder()
                .id(ruleId).user(buildUser()).type("EXPENSE")
                .amount(new BigDecimal("50.00")).currency("EUR")
                .fromAccount(fromAccount).frequency("MONTHLY").intervalValue(1)
                .startDate(LocalDate.now()).nextOccurrence(originalNext).active(true).build();

        RecurringRuleRequest req = expenseRequest(fromId, categoryId);
        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.of(existing));
        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(Optional.of(fromAccount));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringRuleResponse result = recurringRuleService.updateRule(userId, ruleId, req);

        assertThat(result.nextOccurrence()).isEqualTo(originalNext);
    }

    @Test
    void updateRule_notFound_throwsResourceNotFound() {
        UUID ruleId = UUID.randomUUID();
        RecurringRuleRequest req = expenseRequest(UUID.randomUUID(), UUID.randomUUID());
        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringRuleService.updateRule(userId, ruleId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRule_success() {
        UUID ruleId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        RecurringRule rule = RecurringRule.builder()
                .id(ruleId).user(buildUser()).type("EXPENSE")
                .amount(BigDecimal.TEN).currency("EUR")
                .fromAccount(buildAccount(fromId, "Checking"))
                .frequency("MONTHLY").intervalValue(1)
                .startDate(LocalDate.now()).nextOccurrence(LocalDate.now()).active(true).build();
        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.of(rule));

        recurringRuleService.deleteRule(userId, ruleId);

        verify(ruleRepository).delete(rule);
    }

    @Test
    void deleteRule_notFound_throwsResourceNotFound() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringRuleService.deleteRule(userId, ruleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toggleRule_trueToFalse() {
        UUID ruleId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        RecurringRule rule = RecurringRule.builder()
                .id(ruleId).user(buildUser()).type("EXPENSE")
                .amount(BigDecimal.TEN).currency("EUR")
                .fromAccount(buildAccount(fromId, "Checking"))
                .frequency("MONTHLY").intervalValue(1)
                .startDate(LocalDate.now()).nextOccurrence(LocalDate.now()).active(true).build();
        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringRuleResponse result = recurringRuleService.toggleRule(userId, ruleId);

        assertThat(result.active()).isFalse();
    }

    @Test
    void toggleRule_falseToTrue() {
        UUID ruleId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        RecurringRule rule = RecurringRule.builder()
                .id(ruleId).user(buildUser()).type("EXPENSE")
                .amount(BigDecimal.TEN).currency("EUR")
                .fromAccount(buildAccount(fromId, "Checking"))
                .frequency("MONTHLY").intervalValue(1)
                .startDate(LocalDate.now()).nextOccurrence(LocalDate.now()).active(false).build();
        when(ruleRepository.findByIdAndUserId(ruleId, userId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecurringRuleResponse result = recurringRuleService.toggleRule(userId, ruleId);

        assertThat(result.active()).isTrue();
    }
}
