package com.maylee.privatelog.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.maylee.privatelog.entity.Comments;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {

    List<Comments> findByPostId(Long postId);

    @Modifying
    @Query(value = "DELETE FROM comments WHERE post_id = :postId", nativeQuery = true)
    void hardDeleteAllByPostId(@Param("postId") Long postId);
}
