package com.quartz.checkin.dto.common.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimplePageRequest {

    @Min(value = 1, message = "Page 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    private Integer size = 10;
}
