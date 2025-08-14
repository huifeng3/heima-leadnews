package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmSensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;

public interface WmSensitiveService extends IService<WmSensitive> {

    /**
     * 分页查询敏感词列表
     * @param dto
     * @return
     */
    public ResponseResult findList(WmSensitiveDto dto);

    /**
     * 新增敏感词
     * @param adSensitive
     * @return
     */
    ResponseResult saveSensitive(AdSensitive adSensitive);

    /**
     * 删除敏感词
     * @param id
     * @return
     */
    ResponseResult delSensitive(Integer id);

    /**
     * 修改敏感词
     * @param adSensitive
     * @return
     */
    ResponseResult updateSensitive(AdSensitive adSensitive);
}
