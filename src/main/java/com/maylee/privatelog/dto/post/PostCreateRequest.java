package com.maylee.privatelog.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PostCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        boolean isPublic,
        Long categoryId,
        List<Long> tagIds,
        LocalDate postDate
) {
    public PostCreateRequest {
        if (tagIds == null) tagIds = List.of();
    }
}
