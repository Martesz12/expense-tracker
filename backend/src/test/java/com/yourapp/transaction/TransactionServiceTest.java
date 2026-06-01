package com.yourapp.transaction;

import com.yourapp.account.Account;
import com.yourapp.account.AccountRepository;
import com.yourapp.category.Category;
import com.yourapp.category.CategoryRepository;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.tag.Tag;
import com.yourapp.tag.TagRepository;
import com.yourapp.transaction.dto.TransactionFilterRequest;
import com.yourapp.transaction.dto.TransactionRequest;
import com.yourapp.transaction.dto.TransactionResponse;
import com.yourapp.user.User;
import com.yourapp.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountRepository accountRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock TagRepository tagRepository;
    @Mock UserRepository userRepository;

    @InjectMocks TransactionService transactionService;

    private final UUID userId = UUID.randomUUID();

    private User buildUser() {
        return User.builder().id(userId).name("Alice").email("alice@example.com").password("pw").build();
    }

    private Account buildAccount(UUID id) {
        return Account.builder().id(id).user(buildUser()).name("Checking").type("CHECKING")
                .currency("EUR").initialBalance(BigDecimal.ZERO).build();
    }

    private Category buildCategory(UUID id) {
        return Category.builder().id(id).user(buildUser()).name("Food").type("EXPENSE").build();
    }

    private Transaction buildTransaction(UUID id, Account from, Category category) {
        return Transaction.builder()
                .id(id)
                .user(buildUser())
                .type("EXPENSE")
                .amount(new BigDecimal("50.00"))
                .currency("EUR")
                .fromAccount(from)
                .category(category)
                .transactionDate(OffsetDateTime.now())
                .tags(new HashSet<>())
                .build();
    }

    @Test
    void getTransaction_success() {
        UUID txId = UUID.randomUUID();
        Account account = buildAccount(UUID.randomUUID());
        Category category = buildCategory(UUID.randomUUID());
        Transaction tx = buildTransaction(txId, account, category);

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(tx));

        TransactionResponse result = transactionService.getTransaction(userId, txId);

        assertThat(result.id()).isEqualTo(txId);
        assertThat(result.type()).isEqualTo("EXPENSE");
    }

    @Test
    void getTransaction_notFound_throwsResourceNotFound() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(userId, txId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTransaction_expense_success() {
        UUID fromAccountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account account = buildAccount(fromAccountId);
        Category category = buildCategory(categoryId);
        category.setUser(buildUser());

        TransactionRequest req = new TransactionRequest(
                "EXPENSE", new BigDecimal("50.00"), "EUR", null,
                fromAccountId, null, categoryId, null, OffsetDateTime.now(), null);

        when(accountRepository.findByIdAndUserId(fromAccountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.getReferenceById(userId)).thenReturn(buildUser());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransactionResponse result = transactionService.createTransaction(userId, req);

        assertThat(result.type()).isEqualTo("EXPENSE");
        assertThat(result.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void createTransaction_transfer_success() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account fromAccount = buildAccount(fromId);
        Account toAccount = buildAccount(toId);
        toAccount.setName("Savings");

        TransactionRequest req = new TransactionRequest(
                "TRANSFER", new BigDecimal("100.00"), "EUR", null,
                fromId, toId, null, null, OffsetDateTime.now(), null);

        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdAndUserId(toId, userId)).thenReturn(Optional.of(toAccount));
        when(userRepository.getReferenceById(userId)).thenReturn(buildUser());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransactionResponse result = transactionService.createTransaction(userId, req);

        assertThat(result.type()).isEqualTo("TRANSFER");
        assertThat(result.toAccountId()).isEqualTo(toId);
    }

    @Test
    void createTransaction_transferMissingToAccount_throwsBadRequest400() {
        TransactionRequest req = new TransactionRequest(
                "TRANSFER", new BigDecimal("100.00"), "EUR", null,
                UUID.randomUUID(), null, null, null, OffsetDateTime.now(), null);

        assertThatThrownBy(() -> transactionService.createTransaction(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createTransaction_expenseMissingCategory_throwsBadRequest400() {
        TransactionRequest req = new TransactionRequest(
                "EXPENSE", new BigDecimal("50.00"), "EUR", null,
                UUID.randomUUID(), null, null, null, OffsetDateTime.now(), null);

        assertThatThrownBy(() -> transactionService.createTransaction(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createTransaction_incomeMissingCategory_throwsBadRequest400() {
        TransactionRequest req = new TransactionRequest(
                "INCOME", new BigDecimal("1000.00"), "EUR", null,
                UUID.randomUUID(), null, null, null, OffsetDateTime.now(), null);

        assertThatThrownBy(() -> transactionService.createTransaction(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createTransaction_fromAccountNotOwned_throwsResourceNotFound() {
        UUID fromId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        TransactionRequest req = new TransactionRequest(
                "EXPENSE", new BigDecimal("50.00"), "EUR", null,
                fromId, null, categoryId, null, OffsetDateTime.now(), null);
        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTransaction_toAccountNotOwned_throwsResourceNotFound() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account fromAccount = buildAccount(fromId);
        TransactionRequest req = new TransactionRequest(
                "TRANSFER", new BigDecimal("100.00"), "EUR", null,
                fromId, toId, null, null, OffsetDateTime.now(), null);
        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdAndUserId(toId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTransaction_categoryNotOwned_throwsResourceNotFound() {
        UUID fromId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account fromAccount = buildAccount(fromId);
        Category otherUserCategory = buildCategory(categoryId);
        otherUserCategory.setUser(User.builder().id(UUID.randomUUID()).build());

        TransactionRequest req = new TransactionRequest(
                "EXPENSE", new BigDecimal("50.00"), "EUR", null,
                fromId, null, categoryId, null, OffsetDateTime.now(), null);
        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(Optional.of(fromAccount));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(otherUserCategory));

        assertThatThrownBy(() -> transactionService.createTransaction(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTransaction_tagCountMismatch_throwsResourceNotFound() {
        UUID fromId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID tagId1 = UUID.randomUUID();
        UUID tagId2 = UUID.randomUUID();
        Account fromAccount = buildAccount(fromId);
        Category category = buildCategory(categoryId);
        category.setUser(buildUser());

        TransactionRequest req = new TransactionRequest(
                "EXPENSE", new BigDecimal("50.00"), "EUR", null,
                fromId, null, categoryId, null, OffsetDateTime.now(), List.of(tagId1, tagId2));
        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(Optional.of(fromAccount));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(tagRepository.findAllByIdInAndUserId(List.of(tagId1, tagId2), userId))
                .thenReturn(List.of(Tag.builder().id(tagId1).name("urgent").build()));

        assertThatThrownBy(() -> transactionService.createTransaction(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateTransaction_success() {
        UUID txId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Account account = buildAccount(fromId);
        Category category = buildCategory(categoryId);
        category.setUser(buildUser());
        Transaction existing = buildTransaction(txId, account, category);

        TransactionRequest req = new TransactionRequest(
                "EXPENSE", new BigDecimal("75.00"), "EUR", null,
                fromId, null, categoryId, "updated note", OffsetDateTime.now(), null);

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(existing));
        when(accountRepository.findByIdAndUserId(fromId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse result = transactionService.updateTransaction(userId, txId, req);

        assertThat(result.amount()).isEqualByComparingTo("75.00");
        assertThat(result.note()).isEqualTo("updated note");
    }

    @Test
    void updateTransaction_notFound_throwsResourceNotFound() {
        UUID txId = UUID.randomUUID();
        UUID fromId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        TransactionRequest req = new TransactionRequest(
                "EXPENSE", new BigDecimal("50.00"), "EUR", null,
                fromId, null, categoryId, null, OffsetDateTime.now(), null);
        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(userId, txId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTransaction_success() {
        UUID txId = UUID.randomUUID();
        Account account = buildAccount(UUID.randomUUID());
        Category category = buildCategory(UUID.randomUUID());
        Transaction tx = buildTransaction(txId, account, category);
        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(tx));

        transactionService.deleteTransaction(userId, txId);

        verify(transactionRepository).delete(tx);
    }

    @Test
    void deleteTransaction_notFound_throwsResourceNotFound() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(userId, txId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void exportCsv_writesHeaderAndRows() throws Exception {
        UUID fromAccountId = UUID.randomUUID();
        Account account = buildAccount(fromAccountId);
        Category category = buildCategory(UUID.randomUUID());
        category.setUser(buildUser());

        Transaction tx = buildTransaction(UUID.randomUUID(), account, category);
        tx.setNote("lunch, with comma");

        when(transactionRepository.findAll(ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Transaction>>any()))
                .thenReturn(List.of(tx));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        when(httpResponse.getWriter()).thenReturn(pw);

        TransactionFilterRequest filters = new TransactionFilterRequest(null, null, null, null, null, null, null);
        transactionService.exportCsv(userId, filters, httpResponse);

        String csv = sw.toString();
        assertThat(csv).contains("id,date,type,amount");
        assertThat(csv).contains("\"lunch, with comma\"");
    }
}
