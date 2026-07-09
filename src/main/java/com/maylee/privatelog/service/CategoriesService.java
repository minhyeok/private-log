package com.maylee.privatelog.service;

import com.maylee.privatelog.dto.category.CategoryCreateRequest;
import com.maylee.privatelog.dto.category.CategoryResponse;
import com.maylee.privatelog.entity.Categories;
import com.maylee.privatelog.repository.CategoriesRepository;
import com.maylee.privatelog.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriesService {

    private final CategoriesRepository categoriesRepository;
    private final PostsRepository postsRepository;

    public List<CategoryResponse> getAllCategories() {
        return categoriesRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        if (categoriesRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다.");
        }

        Categories category = Categories.builder()
                .name(request.name())
                .build();

        return CategoryResponse.from(categoriesRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Categories category = categoriesRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다."));

        if (postsRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("게시글이 존재하는 카테고리는 삭제할 수 없습니다.");
        }

        categoriesRepository.delete(category);
    }
}
