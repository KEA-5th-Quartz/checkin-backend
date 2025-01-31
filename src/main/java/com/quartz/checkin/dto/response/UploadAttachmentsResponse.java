package com.quartz.checkin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UploadAttachmentsResponse {

    private List<Long> attachmentIds;
}
