package com.yourapp.budget;

import com.yourapp.budget.dto.BudgetRequest;
import com.yourapp.budget.dto.BudgetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> list() {
        return ResponseEntity.ok(budgetService.listBudgets(principal()));
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody BudgetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(principal(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody BudgetRequest req) {
        return ResponseEntity.ok(budgetService.updateBudget(principal(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        budgetService.deleteBudget(principal(), id);
        return ResponseEntity.noContent().build();
    }

    private UUID principal() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
