package com.maylee.privatelog.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank String content,
        @Size(max = 10) String password
) {}
