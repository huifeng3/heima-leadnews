package com.heima.comment.service;

import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ArticleCommentService {

    /**
     * 保存评论
     * @param dto
     * @return
     */
    public ResponseResult save(CommentSaveDto dto);
}
