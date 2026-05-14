package com.yourapp.account;

import com.yourapp.account.dto.AccountRequest;
import com.yourapp.account.dto.AccountResponse;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(UUID userId) {
        return accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(userId)
                .stream()
                .map(a -> toResponse(a, accountRepository.computeBalance(a.getId(), userId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return toResponse(account, accountRepository.computeBalance(accountId, userId));
    }

    @Transactional
    public AccountResponse createAccount(UUID userId, AccountRequest req) {
        Account account = Account.builder()
                .user(userRepository.getReferenceById(userId))
                .name(req.name())
                .type(req.type())
                .currency(req.currency())
                .initialBalance(req.initialBalance() != null ? req.initialBalance() : BigDecimal.ZERO)
                .color(req.color())
                .icon(req.icon())
                .build();
        accountRepository.save(account);
        return toResponse(account, account.getInitialBalance());
    }

    @Transactional
    public AccountResponse updateAccount(UUID userId, UUID accountId, AccountRequest req) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        account.setName(req.name());
        account.setType(req.type());
        account.setCurrency(req.currency());
        if (req.initialBalance() != null) account.setInitialBalance(req.initialBalance());
        account.setColor(req.color());
        account.setIcon(req.icon());
        accountRepository.save(account);
        return toResponse(account, accountRepository.computeBalance(accountId, userId));
    }

    @Transactional
    public void archiveAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        account.setArchived(true);
        accountRepository.save(account);
    }

    private AccountResponse toResponse(Account a, BigDecimal balance) {
        return new AccountResponse(
                a.getId(), a.getName(), a.getType(), a.getCurrency(),
                a.getInitialBalance(), balance,
                a.getColor(), a.getIcon(), a.isArchived(),
                a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
