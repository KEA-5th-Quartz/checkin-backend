package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Like;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    void deleteByComment_Id(Long commentId);
    List<Like> getLikesByComment_Id(Long commentId);
    boolean existsByComment_IdAndMember_Id(Long commentId, Long userId);
}
