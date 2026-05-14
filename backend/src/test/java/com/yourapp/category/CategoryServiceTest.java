package com.yourapp.category;

import com.yourapp.category.dto.CategoryRequest;
import com.yourapp.category.dto.CategoryResponse;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.transaction.TransactionRepository;
import com.yourapp.user.User;
import com.yourapp.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CategoryService categoryService;

    private final UUID userId = UUID.randomUUID();

    private User buildUser() {
        return User.builder().id(userId).name("Alice").email("alice@example.com").password("pw").build();
    }

    private Category buildCategory(UUID id, User user) {
        return Category.builder()
                .id(id)
                .user(user)
                .name("Food")
                .type("EXPENSE")
                .icon("🍔")
                .color("#FF0000")
                .build();
    }

    @Test
    void listCategories_returnsSortedList() {
        User user = buildUser();
        Category c1 = buildCategory(UUID.randomUUID(), user);
        Category c2 = buildCategory(UUID.randomUUID(), user);
        c2.setName("Transport");
        when(categoryRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(c1, c2));

        List<CategoryResponse> result = categoryService.listCategories(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Food");
        assertThat(result.get(1).name()).isEqualTo("Transport");
    }

    @Test
    void createCategory_withoutParent_success() {
        CategoryRequest req = new CategoryRequest("Food", "EXPENSE", "🍔", "#FF0000", null);
        when(userRepository.getReferenceById(userId)).thenReturn(buildUser());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoryResponse result = categoryService.createCategory(userId, req);

        assertThat(result.name()).isEqualTo("Food");
        assertThat(result.parentId()).isNull();
    }

    @Test
    void createCategory_withValidParent_success() {
        User user = buildUser();
        UUID parentId = UUID.randomUUID();
        Category parent = buildCategory(parentId, user);
        CategoryRequest req = new CategoryRequest("Fast Food", "EXPENSE", "🍟", "#FF0000", parentId);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoryResponse result = categoryService.createCategory(userId, req);

        assertThat(result.parentId()).isEqualTo(parentId);
    }

    @Test
    void createCategory_parentNotOwned_throwsResourceNotFound() {
        UUID parentId = UUID.randomUUID();
        Category parentOtherUser = buildCategory(parentId, User.builder().id(UUID.randomUUID()).build());
        CategoryRequest req = new CategoryRequest("Fast Food", "EXPENSE", null, null, parentId);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parentOtherUser));

        assertThatThrownBy(() -> categoryService.createCategory(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCategory_success() {
        User user = buildUser();
        UUID categoryId = UUID.randomUUID();
        Category existing = buildCategory(categoryId, user);
        CategoryRequest req = new CategoryRequest("Groceries", "EXPENSE", "🛒", "#00FF00", null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse result = categoryService.updateCategory(userId, categoryId, req);

        assertThat(result.name()).isEqualTo("Groceries");
    }

    @Test
    void updateCategory_notOwned_throwsResourceNotFound() {
        UUID categoryId = UUID.randomUUID();
        Category owned = buildCategory(categoryId, User.builder().id(UUID.randomUUID()).build());
        CategoryRequest req = new CategoryRequest("X", "EXPENSE", null, null, null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> categoryService.updateCategory(userId, categoryId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCategory_success_noTransactions() {
        User user = buildUser();
        UUID categoryId = UUID.randomUUID();
        Category existing = buildCategory(categoryId, user);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(transactionRepository.existsByCategoryIdAndUserId(categoryId, userId)).thenReturn(false);

        categoryService.deleteCategory(userId, categoryId);

        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void deleteCategory_hasTransactions_throwsConflict409() {
        User user = buildUser();
        UUID categoryId = UUID.randomUUID();
        Category existing = buildCategory(categoryId, user);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(transactionRepository.existsByCategoryIdAndUserId(categoryId, userId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(userId, categoryId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void deleteCategory_notOwned_throwsResourceNotFound() {
        UUID categoryId = UUID.randomUUID();
        Category owned = buildCategory(categoryId, User.builder().id(UUID.randomUUID()).build());
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> categoryService.deleteCategory(userId, categoryId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
