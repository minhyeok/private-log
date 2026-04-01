package com.maylee.privatelog.dto.post;

import jakarta.validation.constraints.Size;

import java.util.List;

public record PostUpdateRequest(
        @Size(max = 200) String title,
        String content,
        @Size(max = 500) String summary,
        Boolean isPublic,
        Long categoryId,
        List<Long> tagIds
) {}
