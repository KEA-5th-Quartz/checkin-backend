package com.quartz.checkin.dto.request;

import com.quartz.checkin.utils.validator.ValidRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberRegistrationRequest {
    @NotBlank(message = "사용자 이름은 필수 입력값입니다.")
    @Pattern(
            regexp = "^[a-z]{3,10}\\.[a-z]{1,5}$",
            message = "사용자 이름의 형식이 올바르지 않습니다."
    )
    private String username;

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "이메일의 형식이 올바르지 않습니다."
    )
    private String email;

    @NotNull(message = "권한은 필수 입력값입니다.")
    @ValidRole
    private String role;
}
