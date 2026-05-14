package com.yourapp.budget;

import com.yourapp.budget.dto.BudgetRequest;
import com.yourapp.budget.dto.BudgetResponse;
import com.yourapp.category.Category;
import com.yourapp.category.CategoryRepository;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BudgetResponse> listBudgets(UUID userId) {
        return budgetRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(b -> toResponse(b, userId))
                .toList();
    }

    @Transactional
    public BudgetResponse createBudget(UUID userId, BudgetRequest req) {
        validateRequest(req);
        Category category = findOwnedCategory(req.categoryId(), userId);

        Budget budget = Budget.builder()
                .user(userRepository.getReferenceById(userId))
                .category(category)
                .periodType(req.periodType())
                .periodStart(req.periodStart())
                .periodEnd(req.periodEnd())
                .amountLimit(req.amountLimit())
                .currency(req.currency())
                .build();

        budgetRepository.save(budget);
        return toResponse(budget, userId);
    }

    @Transactional
    public BudgetResponse updateBudget(UUID userId, UUID budgetId, BudgetRequest req) {
        validateRequest(req);
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        Category category = findOwnedCategory(req.categoryId(), userId);

        budget.setCategory(category);
        budget.setPeriodType(req.periodType());
        budget.setPeriodStart(req.periodStart());
        budget.setPeriodEnd(req.periodEnd());
        budget.setAmountLimit(req.amountLimit());
        budget.setCurrency(req.currency());

        budgetRepository.save(budget);
        return toResponse(budget, userId);
    }

    @Transactional
    public void deleteBudget(UUID userId, UUID budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    private void validateRequest(BudgetRequest req) {
        if ("CUSTOM".equals(req.periodType())) {
            if (req.periodStart() == null || req.periodEnd() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "periodStart and periodEnd are required for CUSTOM budgets");
            }
            if (!req.periodStart().isBefore(req.periodEnd())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "periodStart must be before periodEnd");
            }
        }
    }

    private Category findOwnedCategory(UUID categoryId, UUID userId) {
        return categoryRepository.findById(categoryId)
                .filter(c -> c.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private OffsetDateTime[] resolvePeriod(Budget budget) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;

        switch (budget.getPeriodType()) {
            case "MONTHLY" -> {
                start = today.withDayOfMonth(1);
                end = start.plusMonths(1);
            }
            case "WEEKLY" -> {
                start = today.with(DayOfWeek.MONDAY);
                end = start.plusWeeks(1);
            }
            case "CUSTOM" -> {
                start = budget.getPeriodStart();
                end = budget.getPeriodEnd().plusDays(1);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown period type: " + budget.getPeriodType());
        }

        return new OffsetDateTime[]{
                start.atStartOfDay().atOffset(ZoneOffset.UTC),
                end.atStartOfDay().atOffset(ZoneOffset.UTC)
        };
    }

    private BudgetResponse toResponse(Budget budget, UUID userId) {
        OffsetDateTime[] period = resolvePeriod(budget);
        BigDecimal spent = budgetRepository.computeSpent(
                userId, budget.getCategory().getId(), period[0], period[1]);

        BigDecimal percentUsed = BigDecimal.ZERO;
        if (budget.getAmountLimit().compareTo(BigDecimal.ZERO) != 0) {
            percentUsed = spent
                    .multiply(new BigDecimal("100"))
                    .divide(budget.getAmountLimit(), 1, RoundingMode.HALF_UP);
        }

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getPeriodType(),
                budget.getPeriodStart(),
                budget.getPeriodEnd(),
                budget.getAmountLimit(),
                budget.getCurrency(),
                spent,
                percentUsed,
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
