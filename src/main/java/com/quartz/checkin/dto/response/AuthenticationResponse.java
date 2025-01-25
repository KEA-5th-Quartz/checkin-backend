package com.quartz.checkin.dto.response;

import com.quartz.checkin.entity.Role;
import com.quartz.checkin.security.CustomUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {
    private Long memberId;
    private String username;
    private String profilePic;
    private String role;
    private String accessToken;


    public static AuthenticationResponse from(CustomUser user, String accessToken) {

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .memberId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getValue())
                .profilePic(user.getProfilePic())
                .build();
    }


}
