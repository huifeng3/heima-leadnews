package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.article.service.ArticleCollectionService;
import com.heima.common.constants.BeHaviorConstants;
import com.heima.common.constants.HotArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ArticleCollectionServiceImpl implements ArticleCollectionService {

    @Autowired
    private CacheService cacheService;

    // 定义映射关系
    private static final Map<Integer, String> TYPE_PREFIX_MAP = new HashMap<Integer, String>() {{
        put(0, BeHaviorConstants.COLLECTION_ARTICLE);
        put(1, BeHaviorConstants.COLLECTION_DYNAMIC);
    }};

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;


    @Override
    public ResponseResult articleCollection(CollectionBehaviorDto dto) {

        //参数校验
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
        mess.setArticleId(dto.getEntryId());
        mess.setType(UpdateArticleMess.UpdateArticleType.COLLECTION);

        String key = TYPE_PREFIX_MAP.get(Integer.valueOf(dto.getType())) + ":" + dto.getEntryId();
        //判断当前用户是否已经收藏
        if (dto.getOperation() == 0){
            if (cacheService.sIsMember(key, userId)){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不能重复收藏");
            }
            cacheService.sAdd(key, userId);
            mess.setAdd(1);
        } else if (dto.getOperation() == 1){
            if (!cacheService.sIsMember(key, userId)){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不能重复取消收藏");
            }
            cacheService.sRemove(key, userId);
            mess.setAdd(-1);
        }

        //发送消息，数据聚合
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
