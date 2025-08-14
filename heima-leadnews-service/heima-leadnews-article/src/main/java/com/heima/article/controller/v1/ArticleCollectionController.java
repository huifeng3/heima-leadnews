package com.heima.article.controller.v1;

import com.heima.article.service.ArticleCollectionService;
import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collection_behavior")
public class ArticleCollectionController {

    @Autowired
    private ArticleCollectionService articleCollectionService;

    @PostMapping
    public ResponseResult articleCollection(@RequestBody CollectionBehaviorDto dto){
        return articleCollectionService.articleCollection(dto);
    }

}
