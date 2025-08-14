package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.dtos.AdChannelDto;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import org.springframework.web.bind.annotation.RequestBody;

public interface WmChannelService extends IService<WmChannel> {

    /**
     * 查询所有频道
     * @return
     */
    public ResponseResult findAll();

    /**
     * 查询所有频道
     * @return
     */
    public ResponseResult findList(AdChannelDto dto);

    /**
     * 新增频道
     * @param adChannel
     * @return
     */
    public ResponseResult saveChannel(AdChannel adChannel);

    /**
     * 删除频道
     */
    public ResponseResult delChannel(Integer id);

    /**
     * 修改频道
     * @param adChannel
     * @return
     */
    public ResponseResult updateChannel(AdChannel adChannel);
}