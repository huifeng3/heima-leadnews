package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.common.constants.UserConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.UserAuthMapper;
import com.heima.user.service.UserAuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class UserAuthServiceImpl extends ServiceImpl<UserAuthMapper, ApUserRealname> implements UserAuthService {

    @Autowired
    private ApUserMapper apUserMapper;

    @Autowired
    private IWemediaClient wemediaClient;

    @Override
    public ResponseResult findList(AuthDto dto) {

        //检查参数
        dto.checkParam();

        //分页查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<ApUserRealname> queryWrapper = new LambdaQueryWrapper<>();

        //查询状态
        if(dto.getStatus() != null){
            queryWrapper.eq(ApUserRealname::getStatus,dto.getStatus());
        }

        //按照时间倒序
        queryWrapper.orderByDesc(ApUserRealname::getCreatedTime);
        page = page(page,queryWrapper);

        //返回结果
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Override
    public ResponseResult authAudit(AuthDto dto, Short status) {

        //检查参数
        if(dto == null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 复制属性
        ApUserRealname userRealname = new ApUserRealname();
        userRealname.setId(dto.getId());
        userRealname.setStatus(status);
        userRealname.setUpdatedTime(new Date());
        if (StringUtils.isNotBlank(dto.getMsg())){
            userRealname.setReason(dto.getMsg());
        }

        updateById(userRealname);

        if(status == UserConstants.PASS_AUTH){
            return createWemediaUser(userRealname);
        }

        return ResponseResult.okResult(userRealname.getId());
    }

    private ResponseResult createWemediaUser(ApUserRealname userRealname){

        Integer userRealnameId = userRealname.getId();

        //查询用户认证信息
        ApUserRealname apUserRealname = getById(userRealnameId);
        if (apUserRealname == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"用户不存在");
        }

        Integer userId = apUserRealname.getUserId();
        ApUser apUser = apUserMapper.selectById(userId);
        if (apUser == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"用户不存在");
        }

        WmUser wmUser = wemediaClient.findByName(apUser.getName());
        if (wmUser == null){
            wmUser = new WmUser();
            BeanUtils.copyProperties(apUser, wmUser);
            wmUser.setStatus(9);
            return wemediaClient.saveWmUser(wmUser);
        } else {
            return ResponseResult.okResult("用户已存在，不需添加");
        }
    }

}
