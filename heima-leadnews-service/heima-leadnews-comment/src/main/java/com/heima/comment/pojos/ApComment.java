package com.heima.comment.pojos;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 评论信息表
 * </p>
 *
 */
@Data
@Document("ap_comment")
public class ApComment implements Serializable{

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 评论者ID
     */
    private Integer authorId;

    /**
     * 评论者昵称
     */
    private String authorName;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 文章ID
     */
    private Long entryId;

    private Integer flag;

    /**
     * 点赞数量
     */
    private Integer likes;

    /**
     * 评论数量
     */
    private Integer reply;

    /**
     * 类型
     */
    private Integer type;

}
