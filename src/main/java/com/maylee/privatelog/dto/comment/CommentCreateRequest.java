package com.maylee.privatelog.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank String content,
        boolean isSecret,
        Long parentId,
        @Size(max = 50) String authorName,
        @Size(max = 10) String password
) {}
