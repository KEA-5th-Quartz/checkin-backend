package com.quartz.checkin.repository;

import com.quartz.checkin.dto.member.response.MemberRoleCount;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByUsername(String username);

    Optional<Member> findByRefreshToken(String refreshToken);

    Page<Member> findByRole(Role role, Pageable pageable);

    Page<Member> findByRoleAndUsernameContaining(Role role, String username, Pageable pageable);

    @Query("""
           SELECT new com.quartz.checkin.dto.member.response.MemberRoleCount(
                SUM (CASE WHEN m.role = com.quartz.checkin.entity.Role.USER THEN 1 ELSE 0 END),
                SUM (CASE WHEN m.role = com.quartz.checkin.entity.Role.MANAGER THEN 1 ELSE 0 END),
                SUM (CASE WHEN m.role = com.quartz.checkin.entity.Role.ADMIN THEN 1 ELSE 0 END)
           )
           FROM Member m
           WHERE m.deleted_at IS NULL
            """)
    MemberRoleCount findRoleCounts();
}
