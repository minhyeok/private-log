package com.maylee.privatelog.dto.post;

import com.maylee.privatelog.dto.tag.TagResponse;
import com.maylee.privatelog.entity.Posts;

import java.time.LocalDate;
import java.util.List;

public record PostSummaryResponse(
        Long id,
        String title,
        String contentPreview,
        boolean isPublic,
        int viewCount,
        String categoryName,
        String authorNickname,
        List<TagResponse> tags,
        LocalDate postDate
) {
    public static PostSummaryResponse from(Posts post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                preview(post.getContent()),
                post.isPublic(),
                post.getViewCount(),
                post.getCategory() != null ? post.getCategory().getName() : null,
                post.getUser().getNickname(),
                post.getTags().stream().map(TagResponse::from).toList(),
                post.getPostDate()
        );
    }

    private static String preview(String content) {
        if (content == null || content.length() <= 20) return content;
        return content.substring(0, 20) + "...";
    }
}
