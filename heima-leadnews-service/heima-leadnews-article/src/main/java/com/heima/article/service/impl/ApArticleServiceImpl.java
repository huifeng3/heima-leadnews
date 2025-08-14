package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.BeHaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.ArticleBehaviorVo;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
//import io.seata.spring.annotation.GlobalTransactional;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    private static final short MAX_PAGE_SIZE = 50;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private CacheService cacheService;

    @Override
    public ResponseResult load(Short loadtype, ArticleHomeDto dto) {
        //1.校验参数
        Integer size = dto.getSize();
        if(size == null || size == 0){
            size = 10;
        }
        size = Math.min(size,MAX_PAGE_SIZE);
        dto.setSize(size);

        //类型参数检验
        if(!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE)&&!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            loadtype = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //文章频道校验
        if(StringUtils.isEmpty(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        //时间校验
        if(dto.getMaxBehotTime() == null) dto.setMaxBehotTime(new Date());
        if(dto.getMinBehotTime() == null) dto.setMinBehotTime(new Date());
        //2.查询数据
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, loadtype);

        //TODO: load不了单独用户的数据，因为数据库只支持文章维度的数据，没有文
        // 章和用户的行为对应关系，感觉只能想办法让程序启动的时候redis从磁盘中读取数据


        //3.结果封装
        ResponseResult responseResult = ResponseResult.okResult(apArticles);
        return responseResult;
    }

    /**
     * 加载文章列表
     * @param dto
     * @param type      1 加载更多   2 加载最新
     * @param firstPage true  是首页  flase 非首页
     * @return
     */
    @Override
    public ResponseResult load2(ArticleHomeDto dto, Short type, boolean firstPage) {
        if(firstPage){
            String jsonStr = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if(StringUtils.isNotBlank(jsonStr)){
                List<HotArticleVo> hotArticleVoList = JSON.parseArray(jsonStr, HotArticleVo.class);
                ResponseResult responseResult = ResponseResult.okResult(hotArticleVoList);
                return responseResult;
            }
        }
        return load(type,dto);
    }

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;

    /**
     * 保存app端相关文章
     * @param dto
     * @return
     */
    @Override
//    @GlobalTransactional
    public ResponseResult saveArticle(ArticleDto dto) {
        //参数校验
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto,apArticle);

        //判断是否存在id
        if (dto.getId() == null) {
            //保存文章
            save(apArticle);
            //保存文章配置
            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);
            //保存文章内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(apArticleContent);
        } else {
            //修改文章
            updateById(apArticle);

            //修改文章内容
            LambdaQueryWrapper<ApArticleContent> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ApArticleContent::getArticleId, dto.getId());
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(queryWrapper);

            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);

        }

        //异步调用 生成静态文件上传到minio中
        articleFreemarkerService.buildArticleToMinIO(apArticle,dto.getContent());

        return ResponseResult.okResult(apArticle.getId());
    }

    @Override
    public ApArticle findByName(String title) {
        LambdaQueryWrapper<ApArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApArticle::getTitle,title);
        ApArticle apArticle = getOne(queryWrapper);
        return apArticle;
    }

    @Override
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {
        //参数校验
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //获取用户id
        String userId = AppThreadLocalUtil.getUser().getId().toString();

        //判断是否点赞
        String key = BeHaviorConstants.LIKE_ARTICLE + ":" + dto.getArticleId();
        boolean isLike = cacheService.sIsMember(key, userId);

        //判断是否不喜欢
        String key2 = BeHaviorConstants.UNLIKE_ARTICLE + ":" + dto.getArticleId();
        boolean isUnLike = cacheService.sIsMember(key2, userId);

        //判断是否收藏
        String key3 = BeHaviorConstants.COLLECTION_ARTICLE + ":" + dto.getArticleId();
        boolean isCollection = cacheService.sIsMember(key3, userId);

        //判断是否关注
        String key4 = BeHaviorConstants.FOLLOW_AUTHOR + ":" + dto.getAuthorId();
        boolean isFollow = cacheService.sIsMember(key4, userId);

        return ResponseResult.okResult(new ArticleBehaviorVo(isLike, isUnLike, isCollection, isFollow));
    }

    /**
     * 更新文章的分值  同时更新缓存中的热点文章数据
     * @param mess
     */
    @Override
    public void updateScore(ArticleVisitStreamMess mess) {
        //1.更新文章的阅读、点赞、收藏、评论的数量
        ApArticle apArticle = updateArticle(mess);
        //2.计算文章的分值
        Integer score = computeScore(apArticle);
        score = score * 3;

        //3.替换当前文章对应频道的热点数据
        replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());

        //4.替换推荐对应的热点数据
        replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);
    }

    /*
        * 更新文章行为数据
        *
     */
    private ApArticle updateArticle(ArticleVisitStreamMess mess) {

        ApArticle apArticle = getById(mess.getArticleId());

        apArticle.setViews(apArticle.getViews()==null ? mess.getView() : apArticle.getViews() + mess.getView());
        apArticle.setLikes(apArticle.getLikes()==null ? mess.getLike() : apArticle.getLikes() + mess.getLike());
        apArticle.setCollection(apArticle.getCollection()==null ? mess.getCollect() : apArticle.getCollection() + mess.getCollect());
        apArticle.setComment(apArticle.getComment()==null ? mess.getComment() : apArticle.getComment() + mess.getComment());
        updateById(apArticle);
        return apArticle;

    }

    /*
        计算文章分数
     */
    private Integer computeScore(ApArticle apArticle) {
        Integer score = 0;
        if (apArticle.getLikes() != null){
            score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        if (apArticle.getCollection() != null){
            score += apArticle.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }
        if (apArticle.getComment() != null){
            score += apArticle.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        if (apArticle.getViews() != null){
            score += apArticle.getViews();
        }

        return score;
    }

    /**
     * 替换数据到redis
     * @param apArticle
     * @param score
     * @param key
     */
    private void replaceDataToRedis(ApArticle apArticle, Integer score, String key) {

        String articleListStr = cacheService.get(key);
        if (StringUtils.isNotBlank(articleListStr)){
            List<HotArticleVo> hotArticleList = JSON.parseArray(articleListStr, HotArticleVo.class);
            boolean flag = false;
            //如果缓存中存在该文章，只更新分值
            for (HotArticleVo hotArticleVo : hotArticleList) {
                if (hotArticleVo.getId().equals(apArticle.getId())){
                    hotArticleVo.setScore(score);
                    flag = true;
                    break;
                }
            }
            //如果缓存中不存在该文章，查询缓存中分值最小的文章，如果分值小于当前文章分值，则替换之
            if (!flag){
                if (hotArticleList.size() >= 30) {
                    hotArticleList = hotArticleList.stream()
                            .sorted(Comparator.comparing(HotArticleVo::getScore).reversed())
                            .collect(Collectors.toList());
                    HotArticleVo lastHot = hotArticleList.get(hotArticleList.size() - 1);
                    if (lastHot.getScore() < score) {
                        hotArticleList.remove(lastHot);
                        HotArticleVo hotArticleVo = new HotArticleVo();
                        BeanUtils.copyProperties(apArticle, hotArticleVo);
                        hotArticleVo.setScore(score);
                        hotArticleList.add(hotArticleVo);
                        flag = true;
                    }
                } else {
                    HotArticleVo hotArticleVo = new HotArticleVo();
                    BeanUtils.copyProperties(apArticle, hotArticleVo);
                    hotArticleVo.setScore(score);
                    hotArticleList.add(hotArticleVo);
                    flag = true;
                }
            }
            //缓存到redis
            if (flag){
                hotArticleList = hotArticleList.stream()
                        .sorted(Comparator.comparing(HotArticleVo::getScore).reversed())
                        .collect(Collectors.toList());
                cacheService.set(key, JSON.toJSONString(hotArticleList));
            }
        }
    }

}
