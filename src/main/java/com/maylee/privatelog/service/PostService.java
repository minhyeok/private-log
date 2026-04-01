package com.maylee.privatelog.service;

import com.maylee.privatelog.dto.post.PostCreateRequest;
import com.maylee.privatelog.dto.post.PostDetailResponse;
import com.maylee.privatelog.dto.post.PostSummaryResponse;
import com.maylee.privatelog.dto.post.PostUpdateRequest;
import com.maylee.privatelog.entity.Categories;
import com.maylee.privatelog.entity.Posts;
import com.maylee.privatelog.entity.Tags;
import com.maylee.privatelog.entity.Users;
import com.maylee.privatelog.repository.CategoriesRepository;
import com.maylee.privatelog.repository.PostsRepository;
import com.maylee.privatelog.repository.TagsRepository;
import com.maylee.privatelog.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostsRepository postsRepository;
    private final UsersRepository usersRepository;
    private final CategoriesRepository categoriesRepository;
    private final TagsRepository tagsRepository;
    private final CommentService commentService;

    @Transactional
    public PostDetailResponse createPost(Long userId, PostCreateRequest request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        Categories category = null;
        if (request.categoryId() != null) {
            category = categoriesRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다."));
        }

        List<Tags> tags = request.tagIds().isEmpty()
                ? List.of()
                : tagsRepository.findAllById(request.tagIds());

        Posts post = Posts.builder()
                .user(user)
                .category(category)
                .title(request.title())
                .content(request.content())
                .summary(request.summary())
                .isPublic(request.isPublic())
                .tags(tags)
                .build();

        return PostDetailResponse.from(postsRepository.save(post), List.of());
    }

    @Transactional
    public PostDetailResponse updatePost(Long id, Long userId, PostUpdateRequest request) {
        Posts post = postsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("게시글을 수정할 권한이 없습니다.");
        }

        Categories category = null;
        if (request.categoryId() != null) {
            category = categoriesRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NoSuchElementException("카테고리를 찾을 수 없습니다."));
        }

        List<Tags> tags = null;
        if (request.tagIds() != null) {
            tags = request.tagIds().isEmpty() ? List.of() : tagsRepository.findAllById(request.tagIds());
        }

        post.update(request.title(), request.content(), request.summary(),
                request.isPublic(), category, tags);

        return PostDetailResponse.from(post, commentService.getComments(post.getId()));
    }

    @Transactional
    public void deletePost(Long id) {
        Posts post = postsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        post.delete();
    }

    @Transactional
    public void realDeletePost(Long id) {
        postsRepository.hardDelete(id);
    }

    public PostDetailResponse getPost(Long id) {
        Posts post = postsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        return PostDetailResponse.from(post, commentService.getComments(post.getId()));
    }

    public Page<PostSummaryResponse> getPosts(Pageable pageable) {
        return postsRepository.findAll(pageable)
                .map(PostSummaryResponse::from);
    }

    public PostDetailResponse getPostByDate(LocalDate date) {
        Posts post = postsRepository.findFirstByDate(date)
                .orElseThrow(() -> new NoSuchElementException("해당 날짜의 게시글을 찾을 수 없습니다."));
        return PostDetailResponse.from(post, commentService.getComments(post.getId()));
    }

    public List<PostSummaryResponse> getPostsByMonth(YearMonth yearMonth) {
        return postsRepository.findByCreatedAtBetween(
                        yearMonth.atDay(1).atStartOfDay(),
                        yearMonth.atEndOfMonth().atTime(23, 59, 59),
                        Pageable.unpaged()
                )
                .map(PostSummaryResponse::from)
                .toList();
    }
}
