package com.maylee.privatelog.controller;

import com.maylee.privatelog.dto.comment.CommentCreateRequest;
import com.maylee.privatelog.dto.comment.CommentResponse;
import com.maylee.privatelog.dto.comment.CommentUpdateRequest;
import com.maylee.privatelog.security.AuthUser;
import com.maylee.privatelog.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Long userId = authUser != null ? authUser.id() : null;
        return ResponseEntity.ok(commentService.createComment(postId, userId, request));
    }

    @PatchMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        Long userId = authUser != null ? authUser.id() : null;
        return ResponseEntity.ok(commentService.updateComment(id, userId, request));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String password
    ) {
        Long userId = authUser != null ? authUser.id() : null;
        commentService.deleteComment(id, userId, password);
        return ResponseEntity.noContent().build();
    }
}
