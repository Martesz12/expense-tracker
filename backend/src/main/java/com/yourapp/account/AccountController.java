package com.yourapp.account;

import com.yourapp.account.dto.AccountRequest;
import com.yourapp.account.dto.AccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> list() {
        UUID userId = principal();
        return ResponseEntity.ok(accountService.listAccounts(userId));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest req) {
        UUID userId = principal();
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(userId, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID id) {
        UUID userId = principal();
        return ResponseEntity.ok(accountService.getAccount(userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id, @Valid @RequestBody AccountRequest req) {
        UUID userId = principal();
        return ResponseEntity.ok(accountService.updateAccount(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        UUID userId = principal();
        accountService.archiveAccount(userId, id);
        return ResponseEntity.noContent().build();
    }

    private UUID principal() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
