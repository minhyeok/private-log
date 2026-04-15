package com.maylee.privatelog.dto.archive;

import com.maylee.privatelog.entity.Posts;

public record DiaryDayEntry(
        Long postId,
        String title
) {
    public static DiaryDayEntry from(Posts post) {
        return new DiaryDayEntry(post.getId(), post.getTitle());
    }
}
