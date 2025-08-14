package com.heima.article.service;

import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ArticleCollectionService {

    /**
     * 收藏功能
     * @param dto
     * @return
     */
    ResponseResult articleCollection(CollectionBehaviorDto dto);
}
