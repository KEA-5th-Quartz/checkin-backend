package com.quartz.checkin.security;

import com.quartz.checkin.entity.Role;

import java.time.LocalDateTime;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class CustomUser extends User {

    private Long id;
    private String profilePic;
    private Role role;
    private LocalDateTime passwordChangedAt;

    public CustomUser(Long id, String username, String password, String profilePic, Role role, LocalDateTime passwordChangedAt,
                      Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
        this.profilePic = profilePic;
        this.role = role;
        this.passwordChangedAt = passwordChangedAt;
    }
}
