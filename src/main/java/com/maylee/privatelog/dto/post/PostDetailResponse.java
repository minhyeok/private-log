package com.maylee.privatelog.dto.post;

import com.maylee.privatelog.dto.category.CategoryResponse;
import com.maylee.privatelog.dto.comment.CommentResponse;
import com.maylee.privatelog.dto.tag.TagResponse;
import com.maylee.privatelog.dto.user.UserResponse;
import com.maylee.privatelog.entity.Posts;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        boolean isPublic,
        int viewCount,
        CategoryResponse category,
        UserResponse author,
        List<TagResponse> tags,
        List<CommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse from(Posts post, List<CommentResponse> comments) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.isPublic(),
                post.getViewCount(),
                post.getCategory() != null ? CategoryResponse.from(post.getCategory()) : null,
                UserResponse.from(post.getUser()),
                post.getTags().stream().map(TagResponse::from).toList(),
                comments,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
