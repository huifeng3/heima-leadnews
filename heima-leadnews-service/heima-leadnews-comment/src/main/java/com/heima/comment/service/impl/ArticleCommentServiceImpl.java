package com.heima.comment.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.user.IUserClient;
import com.heima.comment.pojos.ApComment;
import com.heima.comment.service.ArticleCommentService;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

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
}
