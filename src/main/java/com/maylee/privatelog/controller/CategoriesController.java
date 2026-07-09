package com.maylee.privatelog.controller;

import com.maylee.privatelog.dto.category.CategoryCreateRequest;
import com.maylee.privatelog.dto.category.CategoryResponse;
import com.maylee.privatelog.security.AuthUser;
import com.maylee.privatelog.service.CategoriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoriesController {

    private final CategoriesService categoriesService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoriesService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        return ResponseEntity.ok(categoriesService.createCategory(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        categoriesService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
