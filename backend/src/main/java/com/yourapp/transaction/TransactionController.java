package com.yourapp.transaction;

import com.yourapp.common.dto.PageResponse;
import com.yourapp.transaction.dto.TransactionFilterRequest;
import com.yourapp.transaction.dto.TransactionRequest;
import com.yourapp.transaction.dto.TransactionResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) List<UUID> tagIds,
            @RequestParam(required = false) String search,
            HttpServletResponse response) throws IOException {
        TransactionFilterRequest filters = new TransactionFilterRequest(
                from, to, accountId, categoryId, type, tagIds, search);
        transactionService.exportCsv(principal(), filters, response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) List<UUID> tagIds,
            @RequestParam(required = false) String search) {
        TransactionFilterRequest filters = new TransactionFilterRequest(
                from, to, accountId, categoryId, type, tagIds, search);
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("transactionDate").descending());
        return ResponseEntity.ok(transactionService.listTransactions(principal(), filters, pageable));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(principal(), req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransaction(principal(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(transactionService.updateTransaction(principal(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.deleteTransaction(principal(), id);
        return ResponseEntity.noContent().build();
    }

    private UUID principal() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
