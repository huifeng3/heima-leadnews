package com.heima.comment.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.user.IUserClient;
import com.heima.comment.pojos.ApComment;
import com.heima.comment.pojos.ApCommentLike;
import com.heima.comment.pojos.CommentVo;
import com.heima.comment.service.ArticleCommentService;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.comment.dtos.CommentDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ArticleCommentServiceImpl implements ArticleCommentService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IUserClient userClient;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Override
    public ResponseResult save(CommentSaveDto dto) {
        // 参数校验
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //检测用户是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        Integer userId = user.getId();
        String userName = userClient.findUserById(userId).getName();

        // 检验文本长度
        if (dto.getContent().length() > 140){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"评论内容过长");
        }

        // TODO 检测文本敏感词

        // 2.保存数据
        ApComment apComment = new ApComment();
        apComment.setEntryId(dto.getArticleId());
        apComment.setContent(dto.getContent());
        apComment.setAuthorId(userId);
        apComment.setAuthorName(userName);
        apComment.setCreatedTime(new Date());
        apComment.setFlag(0);
        apComment.setType(0);
        apComment.setLikes(0);
        apComment.setReply(0);

        mongoTemplate.save(apComment);

        // 发送消息，进行聚合
        UpdateArticleMess mess = new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        mess.setType(UpdateArticleMess.UpdateArticleType.COMMENT);
        mess.setAdd(1);
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult loadList(CommentDto dto) {
        // 1.参数校验
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 2.查询数据
        int pageSize = 10;
        int pageNum = dto.getIndex() - 1;
        int skip = pageNum * pageSize;

        //构建查询条件
        Query query = new Query();
        query.addCriteria(Criteria.where("entryId").is(dto.getArticleId()));

        //添加时间条件
        query.addCriteria(Criteria.where("createdTime").lt(dto.getMinDate()));
        query.skip(skip).limit(pageSize);
        query.with(Sort.by(Sort.Direction.DESC, "createdTime"));

        //执行查询
        List<ApComment> commentList = mongoTemplate.find(query, ApComment.class);

        //获取用户Id
        Integer userId = AppThreadLocalUtil.getUser().getId();

        // 5.转换为CommentVo并设置点赞状态
        List<CommentVo> commentVoList = commentList.stream().map(apComment -> {
            CommentVo commentVo = new CommentVo();
            // 拷贝属性
            BeanUtils.copyProperties(apComment, commentVo);

            // 查询用户是否点赞了该评论
            Query likeQuery = new Query();
            likeQuery.addCriteria(Criteria.where("authorId").is(userId)
                    .and("commentId").is(apComment.getId()));
            ApCommentLike commentLike = mongoTemplate.findOne(likeQuery, ApCommentLike.class);

            // 如果找到了点赞记录，则设置operation为0（已点赞）
            if(commentLike != null) {
                commentVo.setOperation((short) 0);
            } else {
                commentVo.setOperation(null);
            }

            return commentVo;
        }).collect(Collectors.toList());

        return ResponseResult.okResult(commentVoList);
    }

    @Override
    public ResponseResult like(CommentLikeDto dto) {
        // 1.参数校验
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //获取用户 id
        Integer userId = AppThreadLocalUtil.getUser().getId();

        Query apCommentQuery = new Query();
        apCommentQuery.addCriteria(Criteria.where("id").is(dto.getCommentId()));
        ApComment apComment = mongoTemplate.findOne(apCommentQuery, ApComment.class);
        if (apComment == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"评论不存在");
        }

        Query queryLike = new Query();
        queryLike.addCriteria(Criteria.where("authorId").is(userId)
                .and("commentId").is(dto.getCommentId()));
        ApCommentLike commentLike = mongoTemplate.findOne(queryLike, ApCommentLike.class);


        // 根据operation 判断是点赞还是取消点赞
        if (dto.getOperation() == 0) {
            //点赞
            //判断是否已经点赞
            if (commentLike != null){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"不能重复点赞");
            }
            //增加点赞数量
            apComment.setLikes(apComment.getLikes() + 1);
            mongoTemplate.save(apComment);
            //保存点赞数据
            ApCommentLike apCommentLike = new ApCommentLike();
            apCommentLike.setCommentId(apComment.getId());
            apCommentLike.setAuthorId(userId);
            mongoTemplate.save(apCommentLike);
        } else {
            if (commentLike == null){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"不能重复取消点赞");
            }
            //取消点赞
            apComment.setLikes(apComment.getLikes() - 1);
            mongoTemplate.save(apComment);

            mongoTemplate.remove(queryLike, ApCommentLike.class);
        }

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("likes", apComment.getLikes());

        return ResponseResult.okResult(result);
    }
}
