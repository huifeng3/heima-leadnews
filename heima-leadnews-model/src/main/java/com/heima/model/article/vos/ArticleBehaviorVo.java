package com.heima.model.article.vos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArticleBehaviorVo {

    @JsonProperty("islike")
    private boolean isLike;

    @JsonProperty("isunlike")
    private boolean isUnLike;

    @JsonProperty("iscollection")
    private boolean isCollection;

    @JsonProperty("isfollow")
    private boolean isFollow;

}
