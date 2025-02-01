package com.quartz.checkin.service;

import com.quartz.checkin.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateAttachmentService {

    private final TemplateRepository templateRepository;

}
