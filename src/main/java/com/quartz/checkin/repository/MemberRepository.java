package com.quartz.checkin.repository;

import com.quartz.checkin.dto.member.response.MemberRoleCount;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByDeletedAtIsNotNull();

    Optional<Member> findByEmail(String email);

    Optional<Member> findByUsername(String username);

    Optional<Member> findByRefreshToken(String refreshToken);

    @Query(value = "SELECT m FROM Member m "
            + "WHERE m.role = :role "
            + "AND m.deletedAt IS NULL "
            + "AND m.id != -1",
            countQuery = "SELECT COUNT(m) FROM Member m "
                    + "WHERE m.role = :role "
                    + "AND m.deletedAt IS NULL "
                    + "AND m.id != -1")
    Page<Member> findByRoleAndDeletedAtIsNull(@Param("role") Role role, Pageable pageable);

    @Query(value = "SELECT m FROM Member m "
            + "WHERE m.role = :role "
            + "AND m.username LIKE CONCAT('%', :username, '%') "
            + "AND m.deletedAt IS NULL "
            + "AND m.id != -1",
            countQuery = "SELECT COUNT(m) FROM Member m "
                    + "WHERE m.role = :role "
                    + "AND m.username LIKE CONCAT('%', :username, '%') "
                    + "AND m.deletedAt IS NULL "
                    + "AND m.id != -1")
    Page<Member> findByRoleAndUsernameContainingAndDeletedAtIsNull(
            @Param("role") Role role, @Param("username") String username, Pageable pageable);

    Page<Member> findByDeletedAtIsNotNull(Pageable pageable);

    @Query("""
            SELECT new com.quartz.checkin.dto.member.response.MemberRoleCount(
                 SUM (CASE WHEN m.role = com.quartz.checkin.entity.Role.USER THEN 1 ELSE 0 END),
                 SUM (CASE WHEN m.role = com.quartz.checkin.entity.Role.MANAGER THEN 1 ELSE 0 END),
                 SUM (CASE WHEN m.role = com.quartz.checkin.entity.Role.ADMIN THEN 1 ELSE 0 END)
            )
            FROM Member m
            WHERE m.deletedAt IS NULL
            AND m.id != -1
             """)
    MemberRoleCount findRoleCounts();
}
