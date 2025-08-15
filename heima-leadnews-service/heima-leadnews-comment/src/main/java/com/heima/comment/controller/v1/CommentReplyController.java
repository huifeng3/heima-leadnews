package com.heima.comment.controller.v1;

import com.heima.comment.service.CommentReplyService;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comment_repay")
public class CommentReplyController {

    @Autowired
    private CommentReplyService commentReplyService;

    @PostMapping("/save")
    public ResponseResult saveReply(@RequestBody CommentRepaySaveDto dto){
        return commentReplyService.saveCommentRepay(dto);
    }

    @PostMapping("/load")
    public ResponseResult loadReply(@RequestBody CommentRepayDto dto){
        return commentReplyService.loadReply(dto);
    }

    @PostMapping("/like")
    public ResponseResult likeReply(@RequestBody CommentRepayLikeDto dto){
        return commentReplyService.likeReply(dto);
    }

}
