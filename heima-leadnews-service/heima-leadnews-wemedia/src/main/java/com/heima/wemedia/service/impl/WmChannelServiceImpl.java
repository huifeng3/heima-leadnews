package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.admin.dtos.AdChannelDto;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.service.WmChannelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {

    /**
     * 查询所有频道
     * @return
     */
    @Override
    public ResponseResult findAll() {
        return ResponseResult.okResult(list());
    }

    /**
     * 查询所有频道
     * @return
     */
    @Override
    public ResponseResult findList(AdChannelDto dto) {
        //检查参数
        dto.checkParam();

        //分页查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmChannel> queryWrapper = new LambdaQueryWrapper<>();

        //名称模糊查询
        if (dto.getName() != null && StringUtils.isNotBlank(dto.getName())){
            queryWrapper.like(WmChannel::getName,dto.getName());
        }

        //按照时间倒序
        queryWrapper.orderByDesc(WmChannel::getCreatedTime);
        page = page(page,queryWrapper);

        //返回结果
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    /**
     * 新增频道
     * @param adChannel
     * @return
     */
    @Override
    public ResponseResult saveChannel(AdChannel adChannel) {

        //参数校验
        if (adChannel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        adChannel.setCreatedTime(new Date());
        save(adChannel);

        return ResponseResult.okResult(adChannel.getId());
    }

    @Override
    public ResponseResult delChannel(Integer id) {

        if (id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmChannel wmChannel = getById(id);
        if (wmChannel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        removeById(id);

        return ResponseResult.okResult(id);
    }

    @Override
    public ResponseResult updateChannel(AdChannel adChannel) {

        // 数据校验
        if(adChannel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //数据不存在
        WmChannel wmChannel = getById(adChannel.getId());
        if(wmChannel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        updateById(adChannel);

        return ResponseResult.okResult(adChannel.getId());
    }
}