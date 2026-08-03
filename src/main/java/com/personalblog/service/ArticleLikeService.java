package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalblog.entity.Article;
import com.personalblog.entity.ArticleLike;
import com.personalblog.mapper.ArticleLikeMapper;
import com.personalblog.mapper.ArticleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 帖子点赞
 */
@Service
public class ArticleLikeService {

    private final ArticleLikeMapper likeMapper;
    private final ArticleMapper articleMapper;
    private final NotificationService notificationService;

    public ArticleLikeService(ArticleLikeMapper likeMapper, ArticleMapper articleMapper, NotificationService notificationService) {
        this.likeMapper = likeMapper;
        this.articleMapper = articleMapper;
        this.notificationService = notificationService;
    }

    /** 切换点赞, 返回新状态: true=已赞 */
    public boolean toggle(Long articleId, Long userId) {
        if (articleMapper.selectById(articleId) == null) {
            throw new com.personalblog.common.exception.BusinessException(404, "文章不存在");
        }
        ArticleLike exist = likeMapper.selectOne(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId));
        if (exist != null) {
            likeMapper.deleteById(exist.getId());
            articleMapper.changeLikeCount(articleId, -1);
            return false;
        }
        ArticleLike like = new ArticleLike();
        like.setArticleId(articleId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        likeMapper.insert(like);
        articleMapper.changeLikeCount(articleId, +1);
        Article article = articleMapper.selectById(articleId);
        if (article != null) {
            notificationService.notify(article.getUserId(), userId, "LIKE", articleId, null);
        }
        return true;
    }

    public boolean isLiked(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        return likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId)) > 0;
    }

    public long countByUser(Long userId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getUserId, userId));
    }
}
