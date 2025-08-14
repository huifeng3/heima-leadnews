package com.heima.comment.service;

import com.heima.model.comment.dtos.CommentDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ArticleCommentService {

    /**
     * 保存评论
     * @param dto
     * @return
     */
    public ResponseResult save(CommentSaveDto dto);

    /**
     * 加载评论列表
     * @param dto
     * @return
     */
    ResponseResult loadList(CommentDto dto);

    /**
     * 评论点赞
     * @param dto
     * @return
     */
    ResponseResult like(CommentLikeDto dto);
}
