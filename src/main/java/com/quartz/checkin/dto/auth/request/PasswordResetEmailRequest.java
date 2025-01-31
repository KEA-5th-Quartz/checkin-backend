package com.quartz.checkin.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PasswordResetEmailRequest {

    @NotBlank(message = "사용자 이름은 필수 입력값입니다.")
    @Pattern(
            regexp = "^[a-z]{3,10}\\.[a-z]{1,5}$",
            message = "사용자 이름의 형식이 올바르지 않습니다."
    )
    private String username;
}
