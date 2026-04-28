package com.maylee.privatelog.service;

import com.maylee.privatelog.entity.Posts;
import com.maylee.privatelog.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private final PostsRepository postsRepository;

    public byte[] exportMarkdown(Long categoryId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to   = endDate.atTime(23, 59, 59);

        List<Posts> posts = postsRepository
                .findByCategoryIdAndCreatedAtBetweenOrderByCreatedAtAsc(categoryId, from, to);

        StringBuilder sb = new StringBuilder();
        for (Posts post : posts) {
            sb.append("### ").append(post.getTitle()).append("\n");
            sb.append(post.getContent().strip()).append("\n\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String buildFilename(Long categoryId, LocalDate startDate, LocalDate endDate) {
        return categoryId + "_" + startDate + "_" + endDate + ".md";
    }
}
