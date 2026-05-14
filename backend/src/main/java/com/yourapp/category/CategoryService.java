package com.yourapp.category;

import com.yourapp.category.dto.CategoryRequest;
import com.yourapp.category.dto.CategoryResponse;
import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.transaction.TransactionRepository;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(UUID userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(UUID userId, CategoryRequest req) {
        Category parent = resolveParent(req.parentId(), userId);
        Category category = Category.builder()
                .user(userRepository.getReferenceById(userId))
                .name(req.name())
                .type(req.type())
                .icon(req.icon())
                .color(req.color())
                .parent(parent)
                .build();
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryRequest req) {
        Category category = findOwned(categoryId, userId);
        Category parent = resolveParent(req.parentId(), userId);
        category.setName(req.name());
        category.setType(req.type());
        category.setIcon(req.icon());
        category.setColor(req.color());
        category.setParent(parent);
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId) {
        findOwned(categoryId, userId);
        if (transactionRepository.existsByCategoryIdAndUserId(categoryId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category is referenced by one or more transactions");
        }
        categoryRepository.deleteById(categoryId);
    }

    private Category findOwned(UUID categoryId, UUID userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found");
        }
        return category;
    }

    private Category resolveParent(UUID parentId, UUID userId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .filter(p -> p.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(), c.getName(), c.getType(), c.getIcon(), c.getColor(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
