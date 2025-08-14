package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmSensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmSensitiveService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper, WmSensitive> implements WmSensitiveService {

    @Override
    public ResponseResult findList(WmSensitiveDto dto) {

        // 检查参数
        dto.checkParam();

        // 分页查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmSensitive> queryWrapper = new LambdaQueryWrapper<>();

        if (dto.getName() != null && dto.getName().length() > 0) {
            queryWrapper.like(WmSensitive::getSensitives,dto.getName());
        }
        queryWrapper.orderByDesc(WmSensitive::getCreatedTime);
        page = page(page,queryWrapper);

        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Override
    public ResponseResult saveSensitive(AdSensitive adSensitive) {

        //参数校验
        if (adSensitive == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        adSensitive.setCreatedTime(new Date());
        save(adSensitive);

        return ResponseResult.okResult(adSensitive.getId());
    }

    @Override
    public ResponseResult delSensitive(Integer id) {

        //参数校验
        if (id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmSensitive wmSensitive = getById(id);
        if (wmSensitive == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        removeById(id);

        return ResponseResult.okResult(id);
    }

    @Override
    public ResponseResult updateSensitive(AdSensitive adSensitive) {

        // 1.参数校验
        if (adSensitive == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 2.数据不存在
        WmSensitive wmSensitive = getById(adSensitive.getId());
        if (wmSensitive == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        updateById(adSensitive);

        return ResponseResult.okResult(adSensitive.getId());
    }

}
