package com.yourapp.budget;

import com.yourapp.budget.dto.BudgetRequest;
import com.yourapp.budget.dto.BudgetResponse;
import com.yourapp.category.Category;
import com.yourapp.category.CategoryRepository;
import com.yourapp.common.exception.ResourceNotFoundException;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock BudgetRepository budgetRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;

    @InjectMocks BudgetService budgetService;

    private final UUID userId = UUID.randomUUID();

    private User buildUser() {
        return User.builder().id(userId).name("Alice").email("alice@example.com").password("pw").build();
    }

    private Category buildCategory(UUID id) {
        return Category.builder().id(id).user(buildUser()).name("Food").type("EXPENSE").build();
    }

    private Budget buildMonthlyBudget(UUID id, Category category) {
        return Budget.builder()
                .id(id)
                .user(buildUser())
                .category(category)
                .periodType("MONTHLY")
                .amountLimit(new BigDecimal("500.00"))
                .currency("EUR")
                .build();
    }

    @Test
    void listBudgets_returnsMappedResponses() {
        UUID categoryId = UUID.randomUUID();
        Category category = buildCategory(categoryId);
        Budget budget = buildMonthlyBudget(UUID.randomUUID(), category);

        when(budgetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(budget));
        when(budgetRepository.computeSpent(eq(userId), eq(categoryId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(new BigDecimal("100.00"));

        List<BudgetResponse> result = budgetService.listBudgets(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amountSpent()).isEqualByComparingTo("100.00");
    }

    @Test
    void createBudget_monthly_success() {
        UUID categoryId = UUID.randomUUID();
        Category category = buildCategory(categoryId);
        BudgetRequest req = new BudgetRequest(categoryId, "MONTHLY", null, null, new BigDecimal("500.00"), "EUR");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.getReferenceById(userId)).thenReturn(buildUser());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(budgetRepository.computeSpent(eq(userId), eq(categoryId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        BudgetResponse result = budgetService.createBudget(userId, req);

        assertThat(result.periodType()).isEqualTo("MONTHLY");
        assertThat(result.amountLimit()).isEqualByComparingTo("500.00");
    }

    @Test
    void createBudget_custom_missingPeriodStart_throwsBadRequest400() {
        BudgetRequest req = new BudgetRequest(UUID.randomUUID(), "CUSTOM", null, LocalDate.now().plusDays(7),
                new BigDecimal("500.00"), "EUR");

        assertThatThrownBy(() -> budgetService.createBudget(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createBudget_custom_missingPeriodEnd_throwsBadRequest400() {
        BudgetRequest req = new BudgetRequest(UUID.randomUUID(), "CUSTOM", LocalDate.now(), null,
                new BigDecimal("500.00"), "EUR");

        assertThatThrownBy(() -> budgetService.createBudget(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createBudget_custom_periodStartAfterEnd_throwsBadRequest400() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now();
        BudgetRequest req = new BudgetRequest(UUID.randomUUID(), "CUSTOM", start, end, new BigDecimal("500.00"), "EUR");

        assertThatThrownBy(() -> budgetService.createBudget(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createBudget_custom_periodStartEqualsEnd_throwsBadRequest400() {
        LocalDate same = LocalDate.now();
        BudgetRequest req = new BudgetRequest(UUID.randomUUID(), "CUSTOM", same, same, new BigDecimal("500.00"), "EUR");

        assertThatThrownBy(() -> budgetService.createBudget(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateBudget_success() {
        UUID budgetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = buildCategory(categoryId);
        Budget existing = buildMonthlyBudget(budgetId, category);
        BudgetRequest req = new BudgetRequest(categoryId, "MONTHLY", null, null, new BigDecimal("800.00"), "USD");

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(budgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(budgetRepository.computeSpent(eq(userId), eq(categoryId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        BudgetResponse result = budgetService.updateBudget(userId, budgetId, req);

        assertThat(result.amountLimit()).isEqualByComparingTo("800.00");
        assertThat(result.currency()).isEqualTo("USD");
    }

    @Test
    void updateBudget_notFound_throwsResourceNotFound() {
        UUID budgetId = UUID.randomUUID();
        BudgetRequest req = new BudgetRequest(UUID.randomUUID(), "MONTHLY", null, null, new BigDecimal("500.00"), "EUR");
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudget(userId, budgetId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteBudget_success() {
        UUID budgetId = UUID.randomUUID();
        Category category = buildCategory(UUID.randomUUID());
        Budget budget = buildMonthlyBudget(budgetId, category);
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(userId, budgetId);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void deleteBudget_notFound_throwsResourceNotFound() {
        UUID budgetId = UUID.randomUUID();
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deleteBudget(userId, budgetId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toResponse_percentUsed_calculatesCorrectly() {
        UUID categoryId = UUID.randomUUID();
        Category category = buildCategory(categoryId);
        Budget budget = buildMonthlyBudget(UUID.randomUUID(), category);

        when(budgetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(budget));
        when(budgetRepository.computeSpent(eq(userId), eq(categoryId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(new BigDecimal("250.00"));

        List<BudgetResponse> result = budgetService.listBudgets(userId);

        assertThat(result.get(0).percentUsed()).isEqualByComparingTo("50.0");
    }

    @Test
    void toResponse_zeroAmountLimit_percentUsedIsZero() {
        UUID categoryId = UUID.randomUUID();
        Category category = buildCategory(categoryId);
        Budget budget = Budget.builder()
                .id(UUID.randomUUID()).user(buildUser()).category(category)
                .periodType("MONTHLY").amountLimit(BigDecimal.ZERO).currency("EUR").build();

        when(budgetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(budget));
        when(budgetRepository.computeSpent(eq(userId), eq(categoryId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(new BigDecimal("100.00"));

        List<BudgetResponse> result = budgetService.listBudgets(userId);

        assertThat(result.get(0).percentUsed()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void resolvePeriod_monthly_returnsFirstOfMonthRange() {
        UUID categoryId = UUID.randomUUID();
        Category category = buildCategory(categoryId);
        Budget budget = buildMonthlyBudget(UUID.randomUUID(), category);

        when(budgetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(budget));
        when(budgetRepository.computeSpent(eq(userId), eq(categoryId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenAnswer(inv -> {
                    OffsetDateTime start = inv.getArgument(2);
                    OffsetDateTime end = inv.getArgument(3);
                    assertThat(start.getDayOfMonth()).isEqualTo(1);
                    assertThat(end.getDayOfMonth()).isEqualTo(1);
                    return BigDecimal.ZERO;
                });

        budgetService.listBudgets(userId);
    }

    @Test
    void resolvePeriod_weekly_returnsMondayRange() {
        UUID categoryId = UUID.randomUUID();
        Category category = buildCategory(categoryId);
        Budget budget = Budget.builder()
                .id(UUID.randomUUID()).user(buildUser()).category(category)
                .periodType("WEEKLY").amountLimit(new BigDecimal("200.00")).currency("EUR").build();

        when(budgetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(budget));
        when(budgetRepository.computeSpent(eq(userId), eq(categoryId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenAnswer(inv -> {
                    OffsetDateTime start = inv.getArgument(2);
                    assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
                    return BigDecimal.ZERO;
                });

        budgetService.listBudgets(userId);
    }

    @Test
    void resolvePeriod_custom_addsOneDayToEnd() {
        UUID categoryId = UUID.randomUUID();
        Category category = buildCategory(categoryId);
        LocalDate periodStart = LocalDate.of(2024, 1, 1);
        LocalDate periodEnd = LocalDate.of(2024, 1, 31);
        Budget budget = Budget.builder()
                .id(UUID.randomUUID()).user(buildUser()).category(category)
                .periodType("CUSTOM").periodStart(periodStart).periodEnd(periodEnd)
                .amountLimit(new BigDecimal("300.00")).currency("EUR").build();

        when(budgetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(budget));
        when(budgetRepository.computeSpent(eq(userId), eq(categoryId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenAnswer(inv -> {
                    OffsetDateTime start = inv.getArgument(2);
                    OffsetDateTime end = inv.getArgument(3);
                    assertThat(start.toLocalDate()).isEqualTo(periodStart);
                    assertThat(end.toLocalDate()).isEqualTo(periodEnd.plusDays(1));
                    return BigDecimal.ZERO;
                });

        budgetService.listBudgets(userId);
    }
}
