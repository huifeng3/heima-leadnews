package com.heima.behavior.service;

import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface UserLikeService {

    /**
     * 用户点赞行为
     * @param dto
     * @return
     */
    public ResponseResult userLike(LikesBehaviorDto dto);

}
