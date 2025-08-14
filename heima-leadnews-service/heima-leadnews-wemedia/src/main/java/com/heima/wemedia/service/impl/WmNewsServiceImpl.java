package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.apis.article.IArticleClient;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.*;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.*;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    @Override
    public ResponseResult findAll(WmNewsPageReqDto dto) {
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        WmUser wmUser = WmThreadLocalUtil.getUser();
        if (wmUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //分页条件查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
        if (dto.getStatus() != null){
            queryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }
        if (dto.getChannelId() != null){
            queryWrapper.eq(WmNews::getChannelId,dto.getChannelId());
        }
        if (dto.getBeginPubDate() != null && dto.getEndPubDate() != null ){
            queryWrapper.between(WmNews::getPublishTime,dto.getBeginPubDate(),dto.getEndPubDate());
        }
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            queryWrapper.like(WmNews::getTitle,dto.getKeyword());
        }

        queryWrapper.eq(WmNews::getUserId,wmUser.getId());
        queryWrapper.orderByDesc(WmNews::getCreatedTime);
        page = this.page(page,queryWrapper);

        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;

    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        if (dto == null || dto.getTitle() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 1.保存或修改文章
        WmNews wmNews = new WmNews();
        // 属性拷贝
        BeanUtils.copyProperties(dto,wmNews);
        // 封面图片  list -> string
        if(dto.getImages() != null && dto.getImages().size() > 0) {
            String images = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(images);
        }
        //若封面类型为自动
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);
        }
        saveOrUpdateWmNews(wmNews);

        //判断是否为草稿 为草稿就结束当前方法
        if(dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        List<String> materials = ectractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(materials,wmNews.getId());

        //不为草稿，保存文章封面图片与素材的关系，若布局是自动，需要匹配封面图片
        saveRelativeInfoForCover(dto,wmNews, materials);

//        //审核文章
//        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());

        wmNewsTaskService.addNewsToTask(wmNews.getId(), wmNews.getPublishTime());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

    }

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {
        //1.检查参数
        if (dto == null || dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章
        WmNews wmNews = getById(dto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }

        //3.判断文章是否已发布
        if(!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"只能操作已发布的文章");
        }

        //4.修改文章enable
        if(dto.getEnable() != null && dto.getEnable() > -1 && dto.getEnable() < 2 ) {

            LambdaUpdateWrapper<WmNews> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WmNews::getId,wmNews.getId());
            updateWrapper.set(WmNews::getEnable,dto.getEnable());
            update(updateWrapper);

            if (wmNews.getArticleId() != null) {
                Map<String,Object> map = new HashMap<>();
                map.put("articleId",wmNews.getArticleId());
                map.put("enable",dto.getEnable());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,JSON.toJSONString(map));
            }

        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 列表分页
     * @param dto
     * @return
     */
    @Override
    public ResponseResult listVo(NewsAuthDto dto) {

        //检查参数
        dto.checkParam();

        //分页查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();

        //标题模糊查询
        if (dto.getTitle() != null && StringUtils.isNotBlank(dto.getTitle())){
            queryWrapper.like(WmNews::getTitle,dto.getTitle());
        }

        //状态设置
        if (dto.getStatus() != null){
            queryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }

        //时间倒序
        queryWrapper.orderByDesc(WmNews::getCreatedTime);
        page = page(page,queryWrapper);

        //返回结果
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Autowired
    @Qualifier("com.heima.apis.article.IArticleClient")
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 文章人工审核
     * @param dto
     * @return
     */
    @Override
    public ResponseResult artiAuthAudit(NewsAuthDto dto, Short status) {
        //参数校验
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(dto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //参数设置
        wmNews.setStatus(status);
        wmNews.setReason(dto.getMsg());
        if (status == WemediaConstants.WM_NEWS_ARTI_AUTH_PASS){
            if (articleClient.findByName(wmNews.getTitle()) == null) {
                ArticleDto articleDto = new ArticleDto();
                //属性的拷贝
                BeanUtils.copyProperties(wmNews,articleDto);
                //文章的布局
                articleDto.setLayout(wmNews.getType());
                //频道
                WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
                if(wmChannel != null){
                    articleDto.setChannelName(wmChannel.getName());
                }

                //作者
                articleDto.setAuthorId(wmNews.getUserId().longValue());
                WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
                if(wmUser != null){
                    articleDto.setAuthorName(wmUser.getName());
                }

                //设置文章发布时间
                Date date = new Date();
                wmNews.setPublishTime(date);
                articleDto.setPublishTime(date);

                articleClient.saveArticle(articleDto);

                wmNews.setArticleId(articleDto.getId());
            }
        }
        updateById(wmNews);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 保存或修改新闻
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        //补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short)1);//默认上架

        if(wmNews.getId() == null){
            //保存
            save(wmNews);
        }else {
            //修改
            //删除文章图片与素材的关系
            LambdaQueryWrapper<WmNewsMaterial> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WmNewsMaterial::getNewsId,wmNews.getId());
            wmNewsMaterialMapper.delete(queryWrapper);
            updateById(wmNews);
        }
    }

    /**
     * 提取文章内容中的图片信息
     * @param content
     * @return
     */
    private List<String> ectractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();

        List<Map> maps = JSON.parseArray(content, Map.class);
        for (Map map : maps) {
            if(map.get("type").equals("image")){
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }

        return materials;
    }

    /**
     * 处理文章内容图片与素材的关系
     * @param materials
     * @param newsId
     */
    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials,newsId,WemediaConstants.WM_CONTENT_REFERENCE);
    }

    /**
     * 保存文章图片与素材的关系到数据库中
     * @param materials
     * @param newsId
     * @param type
     */
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
        if(materials!=null && !materials.isEmpty()){
            //通过图片的url查询素材的id
            LambdaQueryWrapper<WmMaterial> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(WmMaterial::getUrl,materials);
            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(queryWrapper);

            //判断素材是否有效
            if(dbMaterials==null || dbMaterials.size() == 0){
                //手动抛出异常   第一个功能：能够提示调用者素材失效了，第二个功能，进行数据的回滚
                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
            }

            if(materials.size() != dbMaterials.size()){
                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FAIL);
            }

            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());

            //批量保存
            wmNewsMaterialMapper.saveRelations(idList,newsId,type);
        }
    }

    /**
     * 第一个功能：如果当前封面类型为自动，则设置封面类型的数据
     * 匹配规则：
     * 1，如果内容图片大于等于1，小于3  单图  type 1
     * 2，如果内容图片大于等于3  多图  type 3
     * 3，如果内容没有图片，无图  type 0
     *
     * 第二个功能：保存封面图片与素材的关系
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {

        List<String> images = dto.getImages();

        // 如果封面类型为自动，则设置封面类型的数据
        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(wmNews.getType())) {
            //多图
            if (materials.size() >= 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else if (materials.size() >= 1 && materials.size() < 3) {
                //单图
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            } else {
                //无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }

            //修改文章
            if (images != null && images.size() > 0) {
                wmNews.setImages(StringUtils.join(images, ","));
            }
            updateById(wmNews);
        }
        if (images != null && images.size() > 0) {
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }

    }

}
