package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.Favorite;
import com.personalblog.mapper.ArticleMapper;
import com.personalblog.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子收藏
 */
@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ArticleMapper articleMapper;
    private final NotificationService notificationService;
    private final ArticleService articleService;

    public FavoriteService(FavoriteMapper favoriteMapper, ArticleMapper articleMapper,
                           NotificationService notificationService, ArticleService articleService) {
        this.favoriteMapper = favoriteMapper;
        this.articleMapper = articleMapper;
        this.notificationService = notificationService;
        this.articleService = articleService;
    }

    /** 我的收藏(分页, 填充展示字段) */
    public IPage<Article> pageFavoritesByUser(Long userId, long current) {
        Page<Favorite> favPage = favoriteMapper.selectPage(
                new Page<>(Math.max(current, 1), 10),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));
        IPage<Article> result = new Page<>(favPage.getCurrent(), favPage.getSize(), favPage.getTotal());
        List<Long> ids = favPage.getRecords().stream().map(Favorite::getArticleId).toList();
        result.setRecords(articleService.listByIdsForDisplay(ids));
        return result;
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
