package com.yourapp.tag;

import com.yourapp.common.exception.ResourceNotFoundException;
import com.yourapp.tag.dto.TagRequest;
import com.yourapp.tag.dto.TagResponse;
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
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TagResponse> listTags(UUID userId) {
        return tagRepository.findByUserIdOrderByNameAsc(userId)
                .stream()
                .map(t -> new TagResponse(t.getId(), t.getName()))
                .toList();
    }

    @Transactional
    public TagResponse createTag(UUID userId, TagRequest req) {
        if (tagRepository.existsByUserIdAndName(userId, req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag name already exists");
        }
        Tag tag = Tag.builder()
                .user(userRepository.getReferenceById(userId))
                .name(req.name())
                .build();
        tagRepository.save(tag);
        return new TagResponse(tag.getId(), tag.getName());
    }

    @Transactional
    public void deleteTag(UUID userId, UUID tagId) {
        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        tagRepository.delete(tag);
    }
}
