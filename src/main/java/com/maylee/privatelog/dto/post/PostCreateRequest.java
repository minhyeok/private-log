package com.maylee.privatelog.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @Size(max = 500) String summary,
        boolean isPublic,
        Long categoryId,
        List<Long> tagIds
) {
    public PostCreateRequest {
        if (tagIds == null) tagIds = List.of();
    }
}
