package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.quartz.checkin.security.CustomUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime passwordChangedAt;


    public static AuthenticationResponse from(CustomUser user, String accessToken) {

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .memberId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getValue())
                .profilePic(user.getProfilePic())
                .passwordChangedAt(user.getPasswordChangedAt())
                .build();
    }

}
