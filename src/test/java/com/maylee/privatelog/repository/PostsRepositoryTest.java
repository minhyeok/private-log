package com.maylee.privatelog.repository;

import com.maylee.privatelog.entity.Categories;
import com.maylee.privatelog.entity.Posts;
import com.maylee.privatelog.entity.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class PostsRepositoryTest {

    @Autowired private EntityManager em;
    @Autowired private PostsRepository postsRepository;

    private Users user;
    private Categories diaryCategory;
    private Categories etcCategory;

    @BeforeEach
    void setUp() {
        user = new Users();
        ReflectionTestUtils.setField(user, "username", "minlee_" + System.nanoTime());
        ReflectionTestUtils.setField(user, "email", System.nanoTime() + "@test.com");
        ReflectionTestUtils.setField(user, "password", "password");
        em.persist(user);

        diaryCategory = Categories.builder().name("일기_" + System.nanoTime()).build();
        etcCategory   = Categories.builder().name("기타_" + System.nanoTime()).build();
        em.persist(diaryCategory);
        em.persist(etcCategory);

        em.flush();
    }

    @Test
    @DisplayName("user, category, tags를 JOIN해서 단건 조회")
    void findWithDetailsById_success() {
        Posts post = Posts.builder()
                .user(user)
                .category(diaryCategory)
                .title("1월 1일")
                .content("새해 일기")
                .isPublic(true)
                .build();
        em.persist(post);
        em.flush();
        em.clear();

        Optional<Posts> result = postsRepository.findWithDetailsById(post.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("1월 1일");
        assertThat(result.get().getUser()).isNotNull();
        assertThat(result.get().getCategory().getName()).startsWith("일기_");
    }

    @Test
    @DisplayName("카테고리 id로 게시글 필터링")
    void findByCategoryId_returnsFilteredPosts() {
        Posts diary = Posts.builder()
                .user(user).category(diaryCategory).title("일기 글").content("내용").isPublic(true).build();
        Posts etc = Posts.builder()
                .user(user).category(etcCategory).title("기타 글").content("내용").isPublic(true).build();
        em.persist(diary);
        em.persist(etc);
        em.flush();

        Page<Posts> result = postsRepository.findByCategoryId(
                diaryCategory.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("일기 글");
    }

    @Test
    @DisplayName("hard delete 후 조회되지 않음")
    void hardDelete_permanentlyDeletesPost() {
        Posts post = Posts.builder()
                .user(user).title("삭제될 글").content("내용").isPublic(true).build();
        em.persist(post);
        em.flush();
        Long id = post.getId();

        postsRepository.hardDelete(id);
        em.flush();
        em.clear();

        Optional<Posts> result = postsRepository.findById(id);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("soft delete된 게시글은 일반 조회에 나타나지 않음")
    void softDelete_notFoundInNormalQuery() {
        Posts post = Posts.builder()
                .user(user).title("숨겨진 글").content("내용").isPublic(true).build();
        em.persist(post);
        em.flush();

        post.delete();
        em.flush();
        em.clear();

        Optional<Posts> result = postsRepository.findById(post.getId());
        assertThat(result).isEmpty();
    }
}
