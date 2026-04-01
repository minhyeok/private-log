package com.maylee.privatelog.service;

import com.maylee.privatelog.dto.comment.CommentCreateRequest;
import com.maylee.privatelog.dto.comment.CommentResponse;
import com.maylee.privatelog.dto.comment.CommentUpdateRequest;
import com.maylee.privatelog.entity.Comments;
import com.maylee.privatelog.entity.Posts;
import com.maylee.privatelog.entity.Users;
import com.maylee.privatelog.repository.CommentsRepository;
import com.maylee.privatelog.repository.PostsRepository;
import com.maylee.privatelog.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentsRepository commentsRepository;
    private final PostsRepository postsRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentCreateRequest request) {
        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        Users user = null;
        if (userId != null) {
            user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        } else if (request.authorName() == null || request.password() == null) {
            throw new IllegalArgumentException("비로그인 댓글은 작성자명과 비밀번호가 필요합니다.");
        }

        Comments parent = null;
        if (request.parentId() != null) {
            parent = commentsRepository.findById(request.parentId())
                    .orElseThrow(() -> new NoSuchElementException("부모 댓글을 찾을 수 없습니다."));
            if (parent.getParent() != null) {
                throw new IllegalArgumentException("답글에는 댓글을 달 수 없습니다.");
            }
        }

        Comments comment = Comments.builder()
                .post(post)
                .user(user)
                .parent(parent)
                .authorName(request.authorName())
                .password(request.password())
                .content(request.content())
                .isSecret(request.isSecret())
                .build();

        return CommentResponse.from(commentsRepository.save(comment), List.of());
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId, String password) {
        Comments comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        verifyAuthority(comment, userId, password);
        comment.delete();
    }

    @Transactional
    public void deleteAllComments(Long postId) {
        commentsRepository.hardDeleteAllByPostId(postId);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentUpdateRequest request) {
        Comments comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        verifyAuthority(comment, userId, request.password());
        comment.update(request.content());

        return CommentResponse.from(comment, List.of());
    }

    public List<CommentResponse> getComments(Long postId) {
        List<Comments> all = commentsRepository.findByPostId(postId);

        Map<Long, List<CommentResponse>> childrenMap = all.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getParent().getId(),
                        Collectors.mapping(c -> CommentResponse.from(c, List.of()), Collectors.toList())
                ));

        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> CommentResponse.from(c, childrenMap.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    private void verifyAuthority(Comments comment, Long userId, String password) {
        if (comment.getUser() != null) {
            if (!comment.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("댓글 작성자만 수정/삭제할 수 있습니다.");
            }
        } else {
            if (!comment.getPassword().equals(password)) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        }
    }
}
