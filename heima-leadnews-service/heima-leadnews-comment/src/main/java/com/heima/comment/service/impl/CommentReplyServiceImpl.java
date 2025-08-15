package com.heima.comment.service.impl;

import com.heima.apis.user.IUserClient;
import com.heima.comment.pojos.ApComment;
import com.heima.comment.pojos.ApCommentRepay;
import com.heima.comment.pojos.ApCommentRepayLike;
import com.heima.comment.pojos.CommentRepayVo;
import com.heima.comment.service.CommentReplyService;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentReplyServiceImpl implements CommentReplyService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IUserClient userClient;

    @Override
    public ResponseResult saveCommentRepay(CommentRepaySaveDto dto) {
        //1.检查参数
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //判断是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        //获取用户id
        Integer userId = user.getId();
        //获取用户名称
        ApUser author = userClient.findUserById(userId);
        String authorName = author.getName();

        //构建评论数据
        ApCommentRepay apCommentRepay = new ApCommentRepay();
        apCommentRepay.setAuthorId(userId);
        apCommentRepay.setAuthorName(authorName);
        apCommentRepay.setContent(dto.getContent());
        apCommentRepay.setCommentId(dto.getCommentId());
        apCommentRepay.setCreatedTime(new Date());
        apCommentRepay.setUpdatedTime(new Date());
        apCommentRepay.setLikes(0);

        //保存数据
        mongoTemplate.save(apCommentRepay);

        //更新文章评论数
        ApComment apComment = mongoTemplate.findOne(Query.query(Criteria.where("id").is(dto.getCommentId())), ApComment.class);
        if (apComment == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        apComment.setReply(apComment.getReply() + 1);
        mongoTemplate.save(apComment);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult loadReply(CommentRepayDto dto) {

        //1.检查参数
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //获取用户id
        Integer userId = AppThreadLocalUtil.getUser().getId();

        //设置查询条件
        Query query = new Query();
        query.addCriteria(Criteria.where("commentId").is(dto.getCommentId()));
        query.addCriteria(Criteria.where("createdTime").lt(dto.getMinDate()));

        if (dto.getSize() != null) {
            query.limit(dto.getSize());
        }

        query.with(Sort.by(Sort.Direction.DESC, "createdTime"));
        List<ApCommentRepay> replyList = mongoTemplate.find(query, ApCommentRepay.class);
        List<CommentRepayVo> commentRepayListVoList = replyList.stream().map(reply -> {
            //创建VO
            CommentRepayVo commentRepayVo = new CommentRepayVo();
            BeanUtils.copyProperties(reply, commentRepayVo);

            Query queryLike = new Query();
            queryLike.addCriteria(Criteria.where("commentRepayId").is(reply.getId())
                    .and("authorId").is(userId));
            ApCommentRepayLike commentRepayLike = mongoTemplate.findOne(queryLike, ApCommentRepayLike.class);
            if (commentRepayLike != null) {
                commentRepayVo.setOperation((short) 0);
            } else {
                commentRepayVo.setOperation(null);
            }

            return commentRepayVo;
        }).collect(Collectors.toList());

        return ResponseResult.okResult(commentRepayListVoList);
    }

    @Override
    public ResponseResult likeReply(CommentRepayLikeDto dto) {
        //参数校验
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        //获取用户id
        Integer userId = user.getId();

        //创建评论回复点赞对象
        ApCommentRepayLike apCommentRepayLike = new ApCommentRepayLike();
        apCommentRepayLike.setAuthorId(userId);
        apCommentRepayLike.setCommentRepayId(dto.getCommentRepayId());

        //查询用户点赞是否存在
        Query query = Query.query(Criteria.where("commentRepayId").is(dto.getCommentRepayId()).and("authorId").is(userId));
        ApCommentRepayLike apCommentRepayLike1 = mongoTemplate.findOne(query, ApCommentRepayLike.class);

        //获取被点赞的评论
        ApCommentRepay apCommentRepay = mongoTemplate.findOne(Query.query(Criteria.where("id").is(dto.getCommentRepayId())), ApCommentRepay.class);
        if (apCommentRepay == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //根据operation判断是否点赞
        if (dto.getOperation() == 0){
            if (apCommentRepayLike1 != null){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"不能重复点赞");
            }

            mongoTemplate.save(apCommentRepayLike);
            apCommentRepay.setLikes(apCommentRepay.getLikes() + 1);
            mongoTemplate.save(apCommentRepay);

        } else {
            if (apCommentRepayLike1 == null){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"不能重复取消点赞");
            }
            mongoTemplate.remove(query, ApCommentRepayLike.class);
            apCommentRepay.setLikes(apCommentRepay.getLikes() - 1);
            mongoTemplate.save(apCommentRepay);
        }

        Map<String,Object> result = new HashMap<>();
        result.put("likes",apCommentRepay.getLikes());
        return ResponseResult.okResult(result);
    }
}
