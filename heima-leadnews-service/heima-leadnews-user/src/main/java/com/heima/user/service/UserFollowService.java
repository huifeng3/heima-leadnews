package com.heima.user.service;

import com.heima.model.behavior.dtos.UserRelationDto;
import com.heima.model.common.dtos.ResponseResult;

public interface UserFollowService {
    /**
     * 用户关注
     * @param dto
     * @return
     */
    ResponseResult userFollow(UserRelationDto dto);
}
