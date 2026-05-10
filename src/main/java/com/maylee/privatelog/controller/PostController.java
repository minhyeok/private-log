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
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(postService.getPost(id, authUser != null));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<PostDetailResponse> getPostByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(postService.getPostByDate(date, authUser != null));
    }

    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> getPosts(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 10, sort = "postDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        boolean authenticated = authUser != null;
        if (categoryId != null) {
            return ResponseEntity.ok(postService.getPostsByCategory(categoryId, pageable, authenticated));
        }
        return ResponseEntity.ok(postService.getPosts(pageable, authenticated));
    }

    @GetMapping("/month/{yearMonth}")
    public ResponseEntity<List<PostSummaryResponse>> getPostsByMonth(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(postService.getPostsByMonth(yearMonth, authUser != null));
    }

    @GetMapping("/archive")
    public ResponseEntity<List<DiaryYearGroup>> getArchive(
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(postService.getArchive(categoryId, authUser != null));
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
