package com.heima.model.behavior.dtos;

import lombok.Data;

@Data
public class UserRelationDto {

    private Long articleId;

    private int authorId;

    private Short operation;

}
