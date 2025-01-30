package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Like;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {

    void deleteByCommentId(Long commentId);
    List<Like> getLikesByCommentId(Long commentId);
    boolean existsByCommentIdAndMemberId(Long commentId, Long userId);
}
