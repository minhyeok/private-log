package com.maylee.privatelog.dto.tag;

import com.maylee.privatelog.entity.Tags;

public record TagResponse(
        Long id,
        String name
) {
    public static TagResponse from(Tags tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
