package com.heima.model.comment.dtos;

import lombok.Data;

@Data
public class CommentSaveDto {

    private Long articleId;

    private String content;
}
