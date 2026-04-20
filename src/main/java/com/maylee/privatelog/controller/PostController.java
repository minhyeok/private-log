package com.maylee.privatelog.controller;

import com.maylee.privatelog.dto.archive.DiaryYearGroup;
import com.maylee.privatelog.dto.post.PostCreateRequest;
import com.maylee.privatelog.dto.post.PostDetailResponse;
import com.maylee.privatelog.dto.post.PostSummaryResponse;
import com.maylee.privatelog.dto.post.PostUpdateRequest;
import com.maylee.privatelog.security.AuthUser;
import com.maylee.privatelog.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostDetailResponse> createPost(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PostCreateRequest request
    ) {
        return ResponseEntity.ok(postService.createPost(authUser.id(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<PostDetailResponse> getPostByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(postService.getPostByDate(date));
    }

    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> getPosts(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (categoryId != null) {
            return ResponseEntity.ok(postService.getPostsByCategory(categoryId, pageable));
        }
        return ResponseEntity.ok(postService.getPosts(pageable));
    }

    @GetMapping("/month/{yearMonth}")
    public ResponseEntity<List<PostSummaryResponse>> getPostsByMonth(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        return ResponseEntity.ok(postService.getPostsByMonth(yearMonth));
    }

    @GetMapping("/archive")
    public ResponseEntity<List<DiaryYearGroup>> getArchive(
            @RequestParam(required = false) Long categoryId
    ) {
        return ResponseEntity.ok(postService.getArchive(categoryId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PostDetailResponse> updatePost(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        return ResponseEntity.ok(postService.updatePost(id, authUser.id(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
