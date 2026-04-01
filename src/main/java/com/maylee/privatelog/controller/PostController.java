package com.maylee.privatelog.controller;

import com.maylee.privatelog.dto.post.PostCreateRequest;
import com.maylee.privatelog.dto.post.PostDetailResponse;
import com.maylee.privatelog.dto.post.PostSummaryResponse;
import com.maylee.privatelog.dto.post.PostUpdateRequest;
import com.maylee.privatelog.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 등록
    @PostMapping
    public ResponseEntity<PostDetailResponse> createPost(
            @RequestParam Long userId,
            @Valid @RequestBody PostCreateRequest request
    ) {
        return ResponseEntity.ok(postService.createPost(userId, request));
    }

    // 게시글 단건 조회 (id)
    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    // 게시글 단건 조회 (날짜)
    @GetMapping("/date/{date}")
    public ResponseEntity<PostDetailResponse> getPostByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(postService.getPostByDate(date));
    }

    // 게시글 페이징 목록 조회
    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> getPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.getPosts(pageable));
    }

    // 특정 월의 게시글 목록 조회
    @GetMapping("/month/{yearMonth}")
    public ResponseEntity<List<PostSummaryResponse>> getPostsByMonth(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        return ResponseEntity.ok(postService.getPostsByMonth(yearMonth));
    }

    // 게시글 수정
    @PatchMapping("/{id}")
    public ResponseEntity<PostDetailResponse> updatePost(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        return ResponseEntity.ok(postService.updatePost(id, userId, request));
    }
}
