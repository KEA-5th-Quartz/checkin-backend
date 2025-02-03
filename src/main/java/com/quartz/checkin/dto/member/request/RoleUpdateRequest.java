package com.quartz.checkin.dto.member.request;

import com.quartz.checkin.common.validator.ValidRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RoleUpdateRequest {

    @NotNull(message = "권한은 필수 입력값입니다.")
    @ValidRole
    private String role;
}
