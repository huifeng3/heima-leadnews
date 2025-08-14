package com.heima.behavior.service;

import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface UserUnLikeService {
    /**
     * 用户不喜欢行为
     * @param dto
     * @return
     */
    ResponseResult userUnLike(UnLikesBehaviorDto dto);
}
