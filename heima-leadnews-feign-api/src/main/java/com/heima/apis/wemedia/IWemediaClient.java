package com.heima.apis.wemedia;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("leadnews-wemedia")
public interface IWemediaClient {

    @PostMapping("/api/v1/wemedia/saveUser")
    public ResponseResult saveWmUser(@RequestBody WmUser wmUser);

    @GetMapping("/api/v1/wemedia/findByName/{name}")
    public WmUser findByName(@PathVariable String name);

    @GetMapping("/api/v1/channel/list")
    public ResponseResult getChannels();

}
