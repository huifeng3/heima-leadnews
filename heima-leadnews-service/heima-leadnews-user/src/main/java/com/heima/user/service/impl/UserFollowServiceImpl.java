package com.heima.user.service.impl;

import com.heima.common.constants.BeHaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.UserRelationDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.user.service.UserFollowService;
import com.heima.utils.thread.AppThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserFollowServiceImpl implements UserFollowService {

    @Autowired
    private CacheService cacheService;

    @Override
    public ResponseResult userFollow(UserRelationDto dto) {
        //参数校验
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //获取用户id
        String userId = AppThreadLocalUtil.getUser().getId().toString();

        String key = BeHaviorConstants.FOLLOW_AUTHOR + ":" + dto.getAuthorId();

        //判断操作类型
        if (dto.getOperation() == 0){
            if (cacheService.sIsMember(key, userId)){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不能重复关注");
            }
            cacheService.sAdd(key, userId);
        } else {
            if (!cacheService.sIsMember(key, userId)){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "不能重复取消关注");
            }
            cacheService.sRemove(key, userId);
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
