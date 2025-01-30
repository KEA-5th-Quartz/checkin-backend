package com.quartz.checkin.entity;

import com.quartz.checkin.config.MemberConfig;
import com.quartz.checkin.dto.request.MemberRegistrationRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String profilePic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String refreshToken;

    private LocalDateTime passwordChangedAt;

    private LocalDateTime deleted_at;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordChangedAt = LocalDateTime.now();
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updateProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    @PrePersist
    public void setDefaultProfilePic() {
        if (this.profilePic == null) {
            this.profilePic = MemberConfig.defaultProfilePic;
        }
    }

    public static Member from(MemberRegistrationRequest memberRegistrationRequest, String encodedPassword) {
        return Member.builder()
                .username(memberRegistrationRequest.getUsername())
                .email(memberRegistrationRequest.getEmail())
                .role(Role.valueOf(memberRegistrationRequest.getRole()))
                .password(encodedPassword)
                .build();
    }
}