package com.yourapp.transaction;

import com.yourapp.account.Account;
import com.yourapp.account.AccountRepository;
import com.yourapp.category.Category;
import com.yourapp.category.CategoryRepository;
import com.yourapp.common.dto.PageResponse;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.tag.Tag;
import com.yourapp.tag.TagRepository;
import com.yourapp.tag.dto.TagResponse;
import com.yourapp.transaction.dto.TransactionFilterRequest;
import com.yourapp.transaction.dto.TransactionRequest;
import com.yourapp.transaction.dto.TransactionResponse;
import com.yourapp.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> listTransactions(UUID userId,
                                                              TransactionFilterRequest filters,
                                                              Pageable pageable) {
        Specification<Transaction> spec = TransactionSpecification.build(
                userId, filters.from(), filters.to(), filters.accountId(),
                filters.categoryId(), filters.type(), filters.tagIds(), filters.search());
        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID userId, UUID transactionId) {
        Transaction t = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return toResponse(t);
    }

    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionRequest req) {
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

        List<Tag> tags = resolveTags(req.tagIds(), userId);

        Transaction transaction = Transaction.builder()
                .user(userRepository.getReferenceById(userId))
                .type(req.type())
                .amount(req.amount())
                .currency(req.currency())
                .exchangeRate(req.exchangeRate())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .category(category)
                .note(req.note())
                .transactionDate(req.transactionDate())
                .tags(new java.util.HashSet<>(tags))
                .build();

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID transactionId, TransactionRequest req) {
        validateCrossFieldRules(req);

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

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

        List<Tag> tags = resolveTags(req.tagIds(), userId);

        transaction.setType(req.type());
        transaction.setAmount(req.amount());
        transaction.setCurrency(req.currency());
        transaction.setExchangeRate(req.exchangeRate());
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setCategory(category);
        transaction.setNote(req.note());
        transaction.setTransactionDate(req.transactionDate());
        transaction.getTags().clear();
        transaction.getTags().addAll(tags);

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        transactionRepository.delete(transaction);
    }

    public void exportCsv(UUID userId, TransactionFilterRequest filters, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");

        Specification<Transaction> spec = TransactionSpecification.build(
                userId, filters.from(), filters.to(), filters.accountId(),
                filters.categoryId(), filters.type(), filters.tagIds(), filters.search());

        List<Transaction> transactions = transactionRepository.findAll(spec);

        PrintWriter writer = response.getWriter();
        writer.println("id,date,type,amount,currency,fromAccount,toAccount,category,note,tags");

        for (Transaction t : transactions) {
            String toAccountName = t.getToAccount() != null ? t.getToAccount().getName() : "";
            String categoryName = t.getCategory() != null ? t.getCategory().getName() : "";
            String tagNames = t.getTags().stream()
                    .map(Tag::getName)
                    .reduce((a, b) -> a + "|" + b)
                    .orElse("");

            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    t.getId(),
                    t.getTransactionDate(),
                    t.getType(),
                    t.getAmount(),
                    t.getCurrency(),
                    csvEscape(t.getFromAccount().getName()),
                    csvEscape(toAccountName),
                    csvEscape(categoryName),
                    csvEscape(t.getNote() != null ? t.getNote() : ""),
                    csvEscape(tagNames)
            );
        }
        writer.flush();
    }

    private void validateCrossFieldRules(TransactionRequest req) {
        if ("TRANSFER".equals(req.type()) && req.toAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "toAccountId is required for TRANSFER transactions");
        }
        if (("INCOME".equals(req.type()) || "EXPENSE".equals(req.type())) && req.categoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "categoryId is required for INCOME and EXPENSE transactions");
        }
    }

    private List<Tag> resolveTags(List<UUID> tagIds, UUID userId) {
        if (tagIds == null || tagIds.isEmpty()) return List.of();
        List<Tag> tags = tagRepository.findAllByIdInAndUserId(tagIds, userId);
        if (tags.size() != tagIds.size()) {
            throw new ResourceNotFoundException("One or more tags not found");
        }
        return tags;
    }

    private TransactionResponse toResponse(Transaction t) {
        List<TagResponse> tagResponses = t.getTags().stream()
                .map(tag -> new TagResponse(tag.getId(), tag.getName()))
                .toList();

        return new TransactionResponse(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getCurrency(),
                t.getExchangeRate(),
                t.getFromAccount().getId(),
                t.getFromAccount().getName(),
                t.getToAccount() != null ? t.getToAccount().getId() : null,
                t.getToAccount() != null ? t.getToAccount().getName() : null,
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getNote(),
                t.getTransactionDate(),
                t.getRecurringRuleId(),
                tagResponses,
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
