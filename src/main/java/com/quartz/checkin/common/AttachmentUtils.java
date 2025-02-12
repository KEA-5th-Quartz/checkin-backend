package com.quartz.checkin.common;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.entity.Attachment;
import com.quartz.checkin.entity.Template;
import com.quartz.checkin.entity.TemplateAttachment;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.AttachmentRepository;
import com.quartz.checkin.repository.TemplateAttachmentRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.service.AttachmentService;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttachmentUtils {

    private final AttachmentRepository attachmentRepository;
    private final TemplateAttachmentRepository templateAttachmentRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final AttachmentService attachmentService;

    public void checkInvalidAttachment(List<Long> attachmentIds, List<Attachment> attachments) {
        if (attachmentIds.size() != attachments.size()) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
    }

    public void handleTemplateAttachments(Template template, List<Long> newAttachmentIds) {
        List<Attachment> newAttachments = fetchAttachments(newAttachmentIds);

        checkInvalidAttachment(newAttachmentIds, newAttachments);

        List<Long> savedAttachmentIds = templateAttachmentRepository.findByTemplate(template)
                .stream()
                .map(ta -> ta.getAttachment().getId())
                .collect(Collectors.toList());

        processAttachmentChanges(template, newAttachmentIds, savedAttachmentIds, newAttachments);
    }

    public void handleTicketAttachments(Ticket ticket, List<Long> newAttachmentIds) {
        List<Attachment> newAttachments = fetchAttachments(newAttachmentIds);

        checkInvalidAttachment(newAttachmentIds, newAttachments);

        List<Long> savedAttachmentIds = ticketAttachmentRepository.findByTicketId(ticket.getId())
                .stream()
                .map(ta -> ta.getAttachment().getId())
                .collect(Collectors.toList());

        processAttachmentChanges(ticket, newAttachmentIds, savedAttachmentIds, newAttachments);
    }

    private List<Attachment> fetchAttachments(List<Long> attachmentIds) {
        return attachmentIds.isEmpty() ? Collections.emptyList() : attachmentRepository.findAllById(attachmentIds);
    }

    private void processAttachmentChanges(Object entity, List<Long> newAttachmentIds,
                                          List<Long> savedAttachmentIds, List<Attachment> newAttachments) {
        List<Long> attachmentIdsToAdd = newAttachmentIds.stream()
                .filter(id -> !savedAttachmentIds.contains(id))
                .toList();

        List<Long> attachmentIdsToRemove = savedAttachmentIds.stream()
                .filter(id -> !newAttachmentIds.contains(id))
                .toList();

        List<Attachment> attachmentsToAdd = newAttachments.stream()
                .filter(a -> attachmentIdsToAdd.contains(a.getId()))
                .toList();

        if (entity instanceof Template template) {
            List<TemplateAttachment> newTemplateAttachments = attachmentsToAdd.stream()
                    .map(a -> new TemplateAttachment(template, a))
                    .collect(Collectors.toList());
            templateAttachmentRepository.saveAll(newTemplateAttachments);

            if (!attachmentIdsToRemove.isEmpty()) {
                templateAttachmentRepository.deleteByTemplateAndAttachmentIds(template, attachmentIdsToRemove);
                attachmentService.deleteAttachments(attachmentIdsToRemove);
            }
        } else if (entity instanceof Ticket ticket) {
            List<TicketAttachment> newTicketAttachments = attachmentsToAdd.stream()
                    .map(a -> new TicketAttachment(ticket, a))
                    .collect(Collectors.toList());
            ticketAttachmentRepository.saveAll(newTicketAttachments);

            if (!attachmentIdsToRemove.isEmpty()) {
                ticketAttachmentRepository.deleteByTicketAndAttachmentIds(ticket, attachmentIdsToRemove);

                List<Long> usedAttachmentIds = ticketAttachmentRepository.findAttachmentIdsInUse(attachmentIdsToRemove);
                List<Long> finalAttachmentsToDelete = attachmentIdsToRemove.stream()
                        .filter(id -> !usedAttachmentIds.contains(id))
                        .collect(Collectors.toList());

                if (!finalAttachmentsToDelete.isEmpty()) {
                    attachmentService.deleteAttachments(finalAttachmentsToDelete);
                }
            }
        }
    }
}
