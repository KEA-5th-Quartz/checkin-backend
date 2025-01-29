package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByUsername(String username);

    Optional<Member> findByRefreshToken(String refreshToken);

    Page<Member> findByRole(Role role, Pageable pageable);

    Page<Member> findByRoleAndUsernameContaining(Role role, String username, Pageable pageable);
}
