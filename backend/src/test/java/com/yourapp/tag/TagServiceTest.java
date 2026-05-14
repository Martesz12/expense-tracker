package com.yourapp.tag;

import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.tag.dto.TagRequest;
import com.yourapp.tag.dto.TagResponse;
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
class TagServiceTest {

    @Mock TagRepository tagRepository;
    @Mock UserRepository userRepository;

    @InjectMocks TagService tagService;

    private final UUID userId = UUID.randomUUID();

    private User buildUser() {
        return User.builder().id(userId).name("Alice").email("alice@example.com").password("pw").build();
    }

    private Tag buildTag(UUID id, String name) {
        return Tag.builder().id(id).user(buildUser()).name(name).build();
    }

    @Test
    void listTags_returnsAllForUser() {
        Tag t1 = buildTag(UUID.randomUUID(), "urgent");
        Tag t2 = buildTag(UUID.randomUUID(), "work");
        when(tagRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(t1, t2));

        List<TagResponse> result = tagService.listTags(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("urgent");
        assertThat(result.get(1).name()).isEqualTo("work");
    }

    @Test
    void createTag_success() {
        TagRequest req = new TagRequest("urgent");
        when(tagRepository.existsByUserIdAndName(userId, "urgent")).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(buildUser());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TagResponse result = tagService.createTag(userId, req);

        assertThat(result.name()).isEqualTo("urgent");
        assertThat(result.id()).isNotNull();
    }

    @Test
    void createTag_duplicateName_throwsConflict409() {
        TagRequest req = new TagRequest("urgent");
        when(tagRepository.existsByUserIdAndName(userId, "urgent")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void deleteTag_success() {
        UUID tagId = UUID.randomUUID();
        Tag tag = buildTag(tagId, "urgent");
        when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));

        tagService.deleteTag(userId, tagId);

        verify(tagRepository).delete(tag);
    }

    @Test
    void deleteTag_notOwned_throwsResourceNotFound() {
        UUID tagId = UUID.randomUUID();
        when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.deleteTag(userId, tagId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
