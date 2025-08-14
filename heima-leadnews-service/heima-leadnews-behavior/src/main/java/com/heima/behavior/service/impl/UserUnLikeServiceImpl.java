package com.heima.behavior.service.impl;

import com.heima.behavior.service.UserUnLikeService;
import com.heima.common.constants.BeHaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserUnLikeServiceImpl implements UserUnLikeService {

    @Autowired
    private CacheService cacheService;

    @Override
    public ResponseResult userUnLike(UnLikesBehaviorDto dto) {

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

        String key = BeHaviorConstants.UNLIKE_ARTICLE + ":" + dto.getArticleId();

        //判断操作类型
        if (dto.getType() == 0){
            if (cacheService.sIsMember(key, userId)){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不能重复不喜欢");
            }
            cacheService.sAdd(key, userId);
        } else {
            if (!cacheService.sIsMember(key, userId)){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不能重复取消不喜欢");
            }
            cacheService.sRemove(key, userId);
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

}
