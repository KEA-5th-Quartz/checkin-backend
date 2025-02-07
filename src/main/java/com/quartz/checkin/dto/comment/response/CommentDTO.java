package com.quartz.checkin.dto.comment.response;

import com.quartz.checkin.entity.Comment;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentDTO {
    private Long id;
    private String content;
    private String author;

    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.author = comment.getMember().getUsername(); // Lazy 로딩 방지
    }
}