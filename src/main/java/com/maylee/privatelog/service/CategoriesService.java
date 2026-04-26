package com.maylee.privatelog.service;

import com.maylee.privatelog.dto.category.CategoryResponse;
import com.maylee.privatelog.repository.CategoriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriesService {

    private final CategoriesRepository categoriesRepository;

    public List<CategoryResponse> getAllCategories() {
        return categoriesRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
