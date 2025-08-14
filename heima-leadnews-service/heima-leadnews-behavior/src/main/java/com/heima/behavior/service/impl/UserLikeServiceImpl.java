package com.heima.behavior.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.article.IArticleClient;
import com.heima.behavior.interceptor.AppTokenInterceptor;
import com.heima.behavior.service.UserLikeService;
import com.heima.common.constants.BeHaviorConstants;
import com.heima.common.constants.HotArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserLikeServiceImpl implements UserLikeService {

    // 定义映射关系
    private static final Map<Integer, String> TYPE_PREFIX_MAP = new HashMap<Integer, String>() {{
        put(0, BeHaviorConstants.LIKE_ARTICLE);
        put(1, BeHaviorConstants.LIKE_DYNAMIC);
        put(2, BeHaviorConstants.LIKE_COMMENT);
    }};

    @Autowired
    private CacheService cacheService;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Override
    public ResponseResult userLike(LikesBehaviorDto dto) {

        // 1.参数校验
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        // 2.获取用户id
        String userId = user.getId().toString();

        UpdateArticleMess mess = new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        mess.setType(UpdateArticleMess.UpdateArticleType.LIKES);

        // 3.判断操作类型
        if (dto.getOperation() != null){
            String prefix = TYPE_PREFIX_MAP.get(Integer.valueOf(dto.getType()));
            String key = prefix + ":" + dto.getArticleId();

            if (dto.getOperation() == 0){
                if (cacheService.sIsMember(key, userId)){
                    return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不能重复点赞");
                }
                cacheService.sAdd(key, userId);
                mess.setAdd(1);
            } else {
                if (!cacheService.sIsMember(key, userId)){
                    return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不能重复取消点赞");
                }
                cacheService.sRemove(key, userId);
                mess.setAdd(-1);
            }
        }

        //发送消息，数据聚合
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC,JSON.toJSONString(mess));

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
