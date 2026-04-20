package com.maylee.privatelog.repository;

import com.maylee.privatelog.entity.Posts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostsRepository extends JpaRepository<Posts, Long> {

    // 카테고리별 페이징 목록
    Page<Posts> findByCategoryId(Long categoryId, Pageable pageable);

    // 날짜 범위 내 Posts 페이징 목록
    Page<Posts> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    // Posts가 존재하는 날짜와 해당 날짜의 Post id 목록 (캘린더 뷰용)
    @Query(
        value = "SELECT CAST(created_at AS DATE) AS post_date, id AS post_id " +
                "FROM posts " +
                "WHERE deleted_at IS NULL " +
                "ORDER BY post_date DESC",
        nativeQuery = true
    )
    List<PostDateProjection> findAllPostDates();

    // user, category, tags를 한 번에 JOIN 조회 (단건 상세 조회용)
    @EntityGraph(attributePaths = {"user", "category", "tags"})
    Optional<Posts> findWithDetailsById(Long id);

    // soft delete된 것 포함 단건 조회
    @Query(value = "SELECT * FROM posts WHERE id = :id", nativeQuery = true)
    Optional<Posts> findByIdIncludeDeleted(@Param("id") Long id);

    // soft delete된 것 포함 전체 페이징 조회
    @Query(
        value = "SELECT * FROM posts ORDER BY created_at DESC",
        countQuery = "SELECT COUNT(*) FROM posts",
        nativeQuery = true
    )
    Page<Posts> findAllIncludeDeleted(Pageable pageable);

    // 특정 날짜의 첫 번째 Post 조회
    @Query(
        value = "SELECT * FROM posts WHERE deleted_at IS NULL AND CAST(created_at AS DATE) = :date LIMIT 1",
        nativeQuery = true
    )
    Optional<Posts> findFirstByDate(@Param("date") LocalDate date);

    // 진짜 삭제
    @Modifying
    @Query(value = "DELETE FROM posts WHERE id = :id", nativeQuery = true)
    void hardDelete(@Param("id") Long id);
}
