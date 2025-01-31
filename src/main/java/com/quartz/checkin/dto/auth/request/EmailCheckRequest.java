package com.quartz.checkin.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailCheckRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "이메일의 형식이 올바르지 않습니다."
    )
    private String email;

}
