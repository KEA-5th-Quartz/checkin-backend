package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Like;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Like getLikeByCommentIdAndMemberId(Long commentId, Long userId);
    void deleteLikeById(Long commentId);
    List<Like> getLikesByCommentId(Long commentId);
    boolean existsByCommentIdAndMemberId(Long commentId, Long userId);
}
