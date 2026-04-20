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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentsRepository commentsRepository;
    @Mock private PostsRepository postsRepository;
    @Mock private UsersRepository usersRepository;

    @InjectMocks private CommentService commentService;

    private Users user;
    private Posts post;
    private Comments parentComment;

    @BeforeEach
    void setUp() {
        user = new Users();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "username", "minlee");
        ReflectionTestUtils.setField(user, "email", "min@test.com");
        ReflectionTestUtils.setField(user, "nickname", "이민혁");

        post = Posts.builder()
                .user(user)
                .title("1월 1일")
                .content("내용")
                .isPublic(true)
                .build();
        ReflectionTestUtils.setField(post, "id", 1L);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());

        parentComment = Comments.builder()
                .post(post)
                .user(user)
                .content("최상위 댓글")
                .build();
        ReflectionTestUtils.setField(parentComment, "id", 1L);
        ReflectionTestUtils.setField(parentComment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(parentComment, "updatedAt", LocalDateTime.now());
    }

    @Test
    @DisplayName("로그인 사용자 댓글 등록 성공")
    void createComment_withUser_success() {
        CommentCreateRequest request = new CommentCreateRequest("댓글 내용", false, null, null, null);
        Comments saved = Comments.builder().post(post).user(user).content("댓글 내용").build();
        ReflectionTestUtils.setField(saved, "id", 2L);
        ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());

        given(postsRepository.findById(1L)).willReturn(Optional.of(post));
        given(usersRepository.findById(1L)).willReturn(Optional.of(user));
        given(commentsRepository.save(any())).willReturn(saved);

        CommentResponse response = commentService.createComment(1L, 1L, request);

        assertThat(response.content()).isEqualTo("댓글 내용");
        assertThat(response.children()).isEmpty();
    }

    @Test
    @DisplayName("비로그인 익명 댓글 등록 성공")
    void createComment_anonymous_success() {
        CommentCreateRequest request = new CommentCreateRequest("익명 댓글", false, null, "익명", "1234");
        Comments saved = Comments.builder()
                .post(post).authorName("익명").password("1234").content("익명 댓글").build();
        ReflectionTestUtils.setField(saved, "id", 2L);
        ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());

        given(postsRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentsRepository.save(any())).willReturn(saved);

        CommentResponse response = commentService.createComment(1L, null, request);

        assertThat(response.authorName()).isEqualTo("익명");
    }

    @Test
    @DisplayName("비로그인 댓글 - 작성자명/비밀번호 없으면 예외")
    void createComment_missingCredentials_throws() {
        CommentCreateRequest request = new CommentCreateRequest("내용", false, null, null, null);
        given(postsRepository.findById(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.createComment(1L, null, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("작성자명");
    }

    @Test
    @DisplayName("답글에 답글 등록 - 1단계 초과 시 예외")
    void createComment_replyToReply_throws() {
        Comments childComment = Comments.builder()
                .post(post).user(user).parent(parentComment).content("답글").build();
        ReflectionTestUtils.setField(childComment, "id", 2L);

        CommentCreateRequest request = new CommentCreateRequest("대댓글", false, 2L, null, null);
        given(postsRepository.findById(1L)).willReturn(Optional.of(post));
        given(usersRepository.findById(1L)).willReturn(Optional.of(user));
        given(commentsRepository.findById(2L)).willReturn(Optional.of(childComment));

        assertThatThrownBy(() -> commentService.createComment(1L, 1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("답글");
    }

    @Test
    @DisplayName("게시글이 없으면 댓글 등록 예외")
    void createComment_postNotFound_throws() {
        CommentCreateRequest request = new CommentCreateRequest("내용", false, null, null, null);
        given(postsRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(99L, 1L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("게시글");
    }

    @Test
    @DisplayName("로그인 사용자 댓글 soft delete 성공")
    void deleteComment_byUser_success() {
        given(commentsRepository.findById(1L)).willReturn(Optional.of(parentComment));

        commentService.deleteComment(1L, 1L, null);

        assertThat(parentComment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 삭제 - 다른 사용자이면 예외")
    void deleteComment_wrongUser_throws() {
        given(commentsRepository.findById(1L)).willReturn(Optional.of(parentComment));

        assertThatThrownBy(() -> commentService.deleteComment(1L, 99L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("작성자");
    }

    @Test
    @DisplayName("익명 댓글 삭제 - 비밀번호 일치")
    void deleteComment_byPassword_success() {
        Comments anonComment = Comments.builder()
                .post(post).authorName("익명").password("1234").content("내용").build();
        ReflectionTestUtils.setField(anonComment, "id", 2L);
        given(commentsRepository.findById(2L)).willReturn(Optional.of(anonComment));

        commentService.deleteComment(2L, null, "1234");

        assertThat(anonComment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("익명 댓글 삭제 - 비밀번호 불일치 시 예외")
    void deleteComment_wrongPassword_throws() {
        Comments anonComment = Comments.builder()
                .post(post).authorName("익명").password("1234").content("내용").build();
        ReflectionTestUtils.setField(anonComment, "id", 2L);
        given(commentsRepository.findById(2L)).willReturn(Optional.of(anonComment));

        assertThatThrownBy(() -> commentService.deleteComment(2L, null, "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호");
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_success() {
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용", null);
        given(commentsRepository.findById(1L)).willReturn(Optional.of(parentComment));

        CommentResponse response = commentService.updateComment(1L, 1L, request);

        assertThat(response.content()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("댓글 트리 조회 - 최상위 댓글에 답글이 포함됨")
    void getComments_buildsTree() {
        Comments child = Comments.builder()
                .post(post).user(user).parent(parentComment).content("답글 내용").build();
        ReflectionTestUtils.setField(child, "id", 2L);
        ReflectionTestUtils.setField(child, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(child, "updatedAt", LocalDateTime.now());

        given(commentsRepository.findByPostId(1L)).willReturn(List.of(parentComment, child));

        List<CommentResponse> result = commentService.getComments(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).children()).hasSize(1);
        assertThat(result.get(0).children().get(0).content()).isEqualTo("답글 내용");
    }
}
