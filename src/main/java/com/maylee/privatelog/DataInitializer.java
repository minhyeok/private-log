package com.maylee.privatelog;

import com.maylee.privatelog.entity.Categories;
import com.maylee.privatelog.repository.CategoriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoriesRepository categoriesRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (categoriesRepository.count() == 0) {
            categoriesRepository.saveAll(List.of(
                    Categories.builder().name("일기").build(),
                    Categories.builder().name("문화").build(),
                    Categories.builder().name("기타").build()
            ));
        }
    }
}
