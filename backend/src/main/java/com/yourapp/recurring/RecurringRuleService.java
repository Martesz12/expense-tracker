package com.yourapp.recurring;

import com.yourapp.account.Account;
import com.yourapp.account.AccountRepository;
import com.yourapp.category.Category;
import com.yourapp.category.CategoryRepository;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.recurring.dto.RecurringRuleRequest;
import com.yourapp.recurring.dto.RecurringRuleResponse;
import com.yourapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecurringRuleService {

    private final RecurringRuleRepository ruleRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RecurringRuleResponse> listRules(UUID userId) {
        return ruleRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecurringRuleResponse createRule(UUID userId, RecurringRuleRequest req) {
        validateCrossFieldRules(req);

        Account fromAccount = accountRepository.findByIdAndUserId(req.fromAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("From account not found"));

        Account toAccount = null;
        if ("TRANSFER".equals(req.type())) {
            toAccount = accountRepository.findByIdAndUserId(req.toAccountId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("To account not found"));
        }

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findById(req.categoryId())
                    .filter(c -> c.getUser().getId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        RecurringRule rule = RecurringRule.builder()
                .user(userRepository.getReferenceById(userId))
                .type(req.type())
                .amount(req.amount())
                .currency(req.currency())
                .exchangeRate(req.exchangeRate())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .category(category)
                .frequency(req.frequency())
                .intervalValue(req.intervalValue())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .nextOccurrence(req.startDate())
                .active(true)
                .build();

        ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public RecurringRuleResponse updateRule(UUID userId, UUID ruleId, RecurringRuleRequest req) {
        validateCrossFieldRules(req);

        RecurringRule rule = ruleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring rule not found"));

        Account fromAccount = accountRepository.findByIdAndUserId(req.fromAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("From account not found"));

        Account toAccount = null;
        if ("TRANSFER".equals(req.type())) {
            toAccount = accountRepository.findByIdAndUserId(req.toAccountId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("To account not found"));
        }

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findById(req.categoryId())
                    .filter(c -> c.getUser().getId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        rule.setType(req.type());
        rule.setAmount(req.amount());
        rule.setCurrency(req.currency());
        rule.setExchangeRate(req.exchangeRate());
        rule.setFromAccount(fromAccount);
        rule.setToAccount(toAccount);
        rule.setCategory(category);
        rule.setFrequency(req.frequency());
        rule.setIntervalValue(req.intervalValue());
        rule.setStartDate(req.startDate());
        rule.setEndDate(req.endDate());

        ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public void deleteRule(UUID userId, UUID ruleId) {
        RecurringRule rule = ruleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring rule not found"));
        ruleRepository.delete(rule);
    }

    @Transactional
    public RecurringRuleResponse toggleRule(UUID userId, UUID ruleId) {
        RecurringRule rule = ruleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring rule not found"));
        rule.setActive(!rule.isActive());
        ruleRepository.save(rule);
        return toResponse(rule);
    }

    private void validateCrossFieldRules(RecurringRuleRequest req) {
        if ("TRANSFER".equals(req.type()) && req.toAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "toAccountId is required for TRANSFER recurring rules");
        }
        if (("INCOME".equals(req.type()) || "EXPENSE".equals(req.type())) && req.categoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "categoryId is required for INCOME and EXPENSE recurring rules");
        }
    }

    private RecurringRuleResponse toResponse(RecurringRule r) {
        return new RecurringRuleResponse(
                r.getId(),
                r.getType(),
                r.getAmount(),
                r.getCurrency(),
                r.getExchangeRate(),
                r.getFromAccount().getId(),
                r.getFromAccount().getName(),
                r.getToAccount() != null ? r.getToAccount().getId() : null,
                r.getToAccount() != null ? r.getToAccount().getName() : null,
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getCategory() != null ? r.getCategory().getName() : null,
                r.getFrequency(),
                r.getIntervalValue(),
                r.getStartDate(),
                r.getEndDate(),
                r.getNextOccurrence(),
                r.isActive(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
