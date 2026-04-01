package com.maylee.privatelog.dto.comment;

import com.maylee.privatelog.entity.Comments;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String authorName,
        String content,
        boolean isSecret,
        Long parentId,
        List<CommentResponse> children,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comments comment, List<CommentResponse> children) {
        return new CommentResponse(
                comment.getId(),
                comment.getUser() != null ? comment.getUser().getNickname() : comment.getAuthorName(),
                comment.getContent(),
                comment.isSecret(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                children,
                comment.getCreatedAt()
        );
    }
}
