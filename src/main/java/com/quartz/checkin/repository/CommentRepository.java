package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Comment;
import com.quartz.checkin.entity.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTicketId(Long ticketId);
    List<Comment> findByMember(Member member);
}
