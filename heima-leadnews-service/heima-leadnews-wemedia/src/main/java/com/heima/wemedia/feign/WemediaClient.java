package com.heima.wemedia.feign;

import com.heima.apis.wemedia.IWemediaClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class WemediaClient implements IWemediaClient {

    @Autowired
    private WmUserService wmUserService;

    @Autowired
    private WmChannelService wmChannelService;

    @Override
    @PostMapping("/api/v1/wemedia/saveUser")
    public ResponseResult saveWmUser(@RequestBody WmUser wmUser) {
        return ResponseResult.okResult(wmUserService.save(wmUser));
    }

    @Override
    @GetMapping("/api/v1/wemedia/findByName/{name}")
    public WmUser findByName(@PathVariable String name) {
        return wmUserService.findByName(name);
    }

    @Override
    @GetMapping("/api/v1/channel/list")
    public ResponseResult getChannels() {
        return wmChannelService.findAll();
    }
}
