package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.Favorite;
import com.personalblog.mapper.ArticleMapper;
import com.personalblog.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 帖子收藏
 */
@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ArticleMapper articleMapper;
    private final NotificationService notificationService;

    public FavoriteService(FavoriteMapper favoriteMapper, ArticleMapper articleMapper, NotificationService notificationService) {
        this.favoriteMapper = favoriteMapper;
        this.articleMapper = articleMapper;
        this.notificationService = notificationService;
    }

    /** 切换收藏, 返回新状态: true=已收藏 */
    public boolean toggle(Long articleId, Long userId) {
        if (articleMapper.selectById(articleId) == null) {
            throw new BusinessException(404, "文章不存在");
        }
        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)
                .eq(Favorite::getUserId, userId));
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            articleMapper.changeFavoriteCount(articleId, -1);
            return false;
        }
        Favorite fav = new Favorite();
        fav.setArticleId(articleId);
        fav.setUserId(userId);
        fav.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(fav);
        articleMapper.changeFavoriteCount(articleId, +1);
        Article article = articleMapper.selectById(articleId);
        if (article != null) {
            notificationService.notify(article.getUserId(), userId, "FAVORITE", articleId, null);
        }
        return true;
    }

    public boolean isFavorited(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)
                .eq(Favorite::getUserId, userId)) > 0;
    }

    public long countByUser(Long userId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId));
    }
}
