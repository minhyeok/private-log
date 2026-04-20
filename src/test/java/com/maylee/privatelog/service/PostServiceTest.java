package com.maylee.privatelog.service;

import com.maylee.privatelog.dto.post.PostCreateRequest;
import com.maylee.privatelog.dto.post.PostDetailResponse;
import com.maylee.privatelog.dto.post.PostUpdateRequest;
import com.maylee.privatelog.entity.Categories;
import com.maylee.privatelog.entity.Posts;
import com.maylee.privatelog.entity.Users;
import com.maylee.privatelog.repository.CategoriesRepository;
import com.maylee.privatelog.repository.PostsRepository;
import com.maylee.privatelog.repository.TagsRepository;
import com.maylee.privatelog.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostsRepository postsRepository;
    @Mock private UsersRepository usersRepository;
    @Mock private CategoriesRepository categoriesRepository;
    @Mock private TagsRepository tagsRepository;
    @Mock private CommentService commentService;

    @InjectMocks private PostService postService;

    private Users user;
    private Categories category;
    private Posts post;

    @BeforeEach
    void setUp() {
        user = new Users();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "username", "minlee");
        ReflectionTestUtils.setField(user, "email", "min@test.com");
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "nickname", "이민혁");

        category = Categories.builder().name("일기").build();
        ReflectionTestUtils.setField(category, "id", 1L);

        post = Posts.builder()
                .user(user)
                .category(category)
                .title("1월 1일")
                .content("새해 첫 일기 내용")
                .isPublic(true)
                .build();
        ReflectionTestUtils.setField(post, "id", 1L);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    @DisplayName("게시글 등록 성공")
    void createPost_success() {
        PostCreateRequest request = new PostCreateRequest("1월 1일", "내용", true, 1L, List.of());
        given(usersRepository.findById(1L)).willReturn(Optional.of(user));
        given(categoriesRepository.findById(1L)).willReturn(Optional.of(category));
        given(postsRepository.save(any())).willReturn(post);

        PostDetailResponse response = postService.createPost(1L, request);

        assertThat(response.title()).isEqualTo("1월 1일");
        then(postsRepository).should().save(any(Posts.class));
    }

    @Test
    @DisplayName("게시글 등록 - 존재하지 않는 사용자")
    void createPost_userNotFound() {
        PostCreateRequest request = new PostCreateRequest("1월 1일", "내용", true, null, List.of());
        given(usersRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(99L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("사용자");
    }

    @Test
    @DisplayName("게시글 등록 - 존재하지 않는 카테고리")
    void createPost_categoryNotFound() {
        PostCreateRequest request = new PostCreateRequest("1월 1일", "내용", true, 99L, List.of());
        given(usersRepository.findById(1L)).willReturn(Optional.of(user));
        given(categoriesRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(1L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("카테고리");
    }

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void getPost_success() {
        given(postsRepository.findWithDetailsById(1L)).willReturn(Optional.of(post));
        given(commentService.getComments(1L)).willReturn(List.of());

        PostDetailResponse response = postService.getPost(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("1월 1일");
    }

    @Test
    @DisplayName("게시글 단건 조회 - 존재하지 않는 게시글")
    void getPost_notFound() {
        given(postsRepository.findWithDetailsById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("게시글");
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_success() {
        PostUpdateRequest request = new PostUpdateRequest("수정된 제목", null, null, null, null);
        given(postsRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentService.getComments(1L)).willReturn(List.of());

        PostDetailResponse response = postService.updatePost(1L, 1L, request);

        assertThat(response.title()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("게시글 수정 - 작성자가 아닌 사용자는 예외")
    void updatePost_unauthorizedUser() {
        PostUpdateRequest request = new PostUpdateRequest("수정된 제목", null, null, null, null);
        given(postsRepository.findById(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(1L, 99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("권한");
    }

    @Test
    @DisplayName("게시글 soft delete")
    void deletePost_softDelete() {
        given(postsRepository.findById(1L)).willReturn(Optional.of(post));

        postService.deletePost(1L);

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("게시글 hard delete")
    void realDeletePost() {
        postService.realDeletePost(1L);

        then(postsRepository).should().hardDelete(1L);
    }

    @Test
    @DisplayName("아카이브 조회 - 카테고리 필터 없음")
    void getArchive_withoutFilter() {
        given(postsRepository.findAll(any(Sort.class))).willReturn(List.of(post));

        var result = postService.getArchive(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).year()).isEqualTo(2026);
        assertThat(result.get(0).months().get(0).month()).isEqualTo(1);
        assertThat(result.get(0).months().get(0).days().get(0).date().getDayOfMonth()).isEqualTo(1);
    }

    @Test
    @DisplayName("아카이브 조회 - 카테고리 필터 적용")
    void getArchive_withCategoryFilter() {
        given(postsRepository.findByCategoryId(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post)));

        var result = postService.getArchive(1L);

        assertThat(result).hasSize(1);
        then(postsRepository).should().findByCategoryId(eq(1L), any(Pageable.class));
    }
}
