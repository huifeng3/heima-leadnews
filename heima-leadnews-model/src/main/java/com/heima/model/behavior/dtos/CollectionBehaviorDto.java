package com.heima.model.behavior.dtos;

import lombok.Data;

import java.util.Date;

@Data
public class CollectionBehaviorDto {

    private Long entryId;

    private Short operation;

    private Date publishedTime;

    private Short type;

}
