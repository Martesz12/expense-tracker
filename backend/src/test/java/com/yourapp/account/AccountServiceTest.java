package com.yourapp.account;

import com.yourapp.account.dto.AccountRequest;
import com.yourapp.account.dto.AccountResponse;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.user.User;
import com.yourapp.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock UserRepository userRepository;

    @InjectMocks AccountService accountService;

    private final UUID userId = UUID.randomUUID();

    private User buildUser() {
        return User.builder().id(userId).name("Alice").email("alice@example.com").password("pw").build();
    }

    private Account buildAccount(UUID id) {
        return Account.builder()
                .id(id)
                .user(buildUser())
                .name("Checking")
                .type("CHECKING")
                .currency("EUR")
                .initialBalance(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void listAccounts_returnsMappedResponses() {
        Account a1 = buildAccount(UUID.randomUUID());
        Account a2 = buildAccount(UUID.randomUUID());
        when(accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(userId)).thenReturn(List.of(a1, a2));
        when(accountRepository.computeBalance(any(), eq(userId))).thenReturn(new BigDecimal("200.00"));

        List<AccountResponse> result = accountService.listAccounts(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Checking");
        assertThat(result.get(0).balance()).isEqualByComparingTo("200.00");
    }

    @Test
    void getAccount_success_returnsResponse() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId);
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.computeBalance(accountId, userId)).thenReturn(new BigDecimal("500.00"));

        AccountResponse result = accountService.getAccount(userId, accountId);

        assertThat(result.id()).isEqualTo(accountId);
        assertThat(result.balance()).isEqualByComparingTo("500.00");
    }

    @Test
    void getAccount_notOwned_throwsResourceNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(userId, accountId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createAccount_nullInitialBalance_defaultsToZero() {
        AccountRequest req = new AccountRequest("Savings", "SAVINGS", "EUR", null, null, null);
        when(userRepository.getReferenceById(userId)).thenReturn(buildUser());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse result = accountService.createAccount(userId, req);

        assertThat(result.initialBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createAccount_withInitialBalance_usesProvidedValue() {
        AccountRequest req = new AccountRequest("Savings", "SAVINGS", "EUR", new BigDecimal("250.00"), null, null);
        when(userRepository.getReferenceById(userId)).thenReturn(buildUser());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse result = accountService.createAccount(userId, req);

        assertThat(result.initialBalance()).isEqualByComparingTo("250.00");
    }

    @Test
    void updateAccount_success_updatesFields() {
        UUID accountId = UUID.randomUUID();
        Account existing = buildAccount(accountId);
        AccountRequest req = new AccountRequest("Updated", "SAVINGS", "USD", new BigDecimal("300.00"), "#fff", "icon");
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.computeBalance(accountId, userId)).thenReturn(new BigDecimal("300.00"));

        AccountResponse result = accountService.updateAccount(userId, accountId, req);

        assertThat(result.name()).isEqualTo("Updated");
        assertThat(result.type()).isEqualTo("SAVINGS");
    }

    @Test
    void updateAccount_notOwned_throwsResourceNotFound() {
        UUID accountId = UUID.randomUUID();
        AccountRequest req = new AccountRequest("X", "CHECKING", "EUR", null, null, null);
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateAccount(userId, accountId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void archiveAccount_success_setsArchivedTrue() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId);
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accountService.archiveAccount(userId, accountId);

        assertThat(account.isArchived()).isTrue();
        verify(accountRepository).save(account);
    }

    @Test
    void archiveAccount_notOwned_throwsResourceNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.archiveAccount(userId, accountId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
