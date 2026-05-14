package com.yourapp.recurring;

import com.yourapp.recurring.dto.RecurringRuleRequest;
import com.yourapp.recurring.dto.RecurringRuleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recurring-rules")
@RequiredArgsConstructor
public class RecurringRuleController {

    private final RecurringRuleService ruleService;

    @GetMapping
    public ResponseEntity<List<RecurringRuleResponse>> list() {
        return ResponseEntity.ok(ruleService.listRules(principal()));
    }

    @PostMapping
    public ResponseEntity<RecurringRuleResponse> create(@Valid @RequestBody RecurringRuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ruleService.createRule(principal(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringRuleResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody RecurringRuleRequest req) {
        return ResponseEntity.ok(ruleService.updateRule(principal(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ruleService.deleteRule(principal(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<RecurringRuleResponse> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(ruleService.toggleRule(principal(), id));
    }

    private UUID principal() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
