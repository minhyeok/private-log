package com.maylee.privatelog.dto.post;

import com.maylee.privatelog.dto.tag.TagResponse;
import com.maylee.privatelog.entity.Posts;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
        Long id,
        String title,
        String summary,
        boolean isPublic,
        int viewCount,
        String categoryName,
        String authorNickname,
        List<TagResponse> tags,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Posts post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.isPublic(),
                post.getViewCount(),
                post.getCategory() != null ? post.getCategory().getName() : null,
                post.getUser().getNickname(),
                post.getTags().stream().map(TagResponse::from).toList(),
                post.getCreatedAt()
        );
    }
}
