package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Like;
import com.quartz.checkin.entity.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Like getLikeByCommentIdAndMemberId(Long commentId, Long userId);
    void deleteLikeById(Long commentId);
    void deleteAllByMember(Member member);
    List<Like> getLikesByCommentId(Long commentId);
    boolean existsByCommentIdAndMemberId(Long commentId, Long userId);
}
