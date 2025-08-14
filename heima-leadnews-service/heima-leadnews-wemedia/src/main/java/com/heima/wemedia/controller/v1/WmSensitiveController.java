package com.heima.wemedia.controller.v1;

import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmSensitiveDto;
import com.heima.wemedia.service.WmSensitiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensitive")
public class WmSensitiveController {

    @Autowired
    private WmSensitiveService wmSensitiveService;

    @PostMapping("/list")
    public ResponseResult list(@RequestBody WmSensitiveDto dto){
        return wmSensitiveService.findList(dto);
    }

    @PostMapping("/save")
    public ResponseResult saveSensitive(@RequestBody AdSensitive adSensitive){
        return wmSensitiveService.saveSensitive(adSensitive);
    }

    @DeleteMapping("/del/{id}")
    public ResponseResult delSensitive(@PathVariable Integer id){
        return wmSensitiveService.delSensitive(id);
    }

    @PostMapping("/update")
    public ResponseResult updateSensitive(@RequestBody AdSensitive adSensitive){
        return wmSensitiveService.updateSensitive(adSensitive);
    }

}
