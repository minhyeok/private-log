package com.maylee.privatelog.dto.category;

import com.maylee.privatelog.entity.Categories;

public record CategoryResponse(
        Long id,
        String name
) {
    public static CategoryResponse from(Categories category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
