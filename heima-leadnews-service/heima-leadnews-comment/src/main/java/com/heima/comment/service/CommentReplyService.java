package com.heima.comment.service;

import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.common.dtos.ResponseResult;

public interface CommentReplyService {

    /**
     * 保存评论回复
     * @param dto
     * @return
     */
    ResponseResult saveCommentRepay(CommentRepaySaveDto dto);

    /**
     * 加载评论回复
     * @param dto
     * @return
     */
    ResponseResult loadReply(CommentRepayDto dto);

    /**
     * 评论回复点赞
     * @param dto
     * @return
     */
    ResponseResult likeReply(CommentRepayLikeDto dto);
}
