package com.quartz.checkin.dto.member.request;

import com.quartz.checkin.common.validator.ValidRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberInfoListRequest {

    @NotNull(message = "권한은 필수 입력값입니다.")
    @ValidRole
    private String role;

    @Min(value = 1, message = "Page 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    private Integer size = 10;

    private String username;
}
