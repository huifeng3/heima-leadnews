package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUserRealname;

public interface UserAuthService extends IService<ApUserRealname> {

    /**
     * 用户分页查询
     * @param dto
     * @return
     */
    public ResponseResult findList(AuthDto dto);

    /**
     * 用户认证失败列表
     * @param dto
     * @return
     */
    public ResponseResult authAudit(AuthDto dto, Short status);
}
