package com.quartz.checkin.dto.member.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "비밀번호 초기화 토큰은 필수 입력값입니다.")
    private String passwordResetToken;

    @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#&()\\-\\[\\]{}:;',?/*~\\$^+=<>.])[A-Za-z\\d!@#&()\\-\\[\\]{}:;',?/*~\\$^+=<>.]{8,}$",
            message = "비밀번호는 특수문자, 알파벳, 숫자를 최소 1자리씩 포함하며 총 길이가 최소 8글자여야 합니다."
    )
    private String newPassword;
}
