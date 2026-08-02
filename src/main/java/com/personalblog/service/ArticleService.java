package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.User;
import com.personalblog.mapper.ArticleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章业务
 */
@Service
public class ArticleService {

    private static final int PAGE_SIZE = 10;

    private final ArticleMapper articleMapper;
    private final CommentService commentService;
    private final UserService userService;

    public ArticleService(ArticleMapper articleMapper, CommentService commentService, UserService userService) {
        this.articleMapper = articleMapper;
        this.commentService = commentService;
        this.userService = userService;
    }

    /** 分页查询文章列表(按创建时间倒序), 并批量填充作者昵称与评论数 */
    public IPage<Article> pageArticles(long current) {
        IPage<Article> page = articleMapper.selectPage(
                new Page<>(Math.max(current, 1), PAGE_SIZE),
                new LambdaQueryWrapper<Article>().orderByDesc(Article::getCreateTime));
        fillAuthors(page.getRecords());
        fillCommentCounts(page.getRecords());
        return page;
    }

    /** 按 ID 查询文章, 不存在抛 404 */
    public Article getById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        fillAuthor(article);
        return article;
    }

    /** 发布文章 */
    public Article create(Long authorId, String title, String summary, String content) {
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedContent = content == null ? "" : content.trim();
        if (trimmedTitle.isEmpty()) {
            throw new BusinessException("标题不能为空");
        }
        if (trimmedTitle.length() > 200) {
            throw new BusinessException("标题不能超过 200 字");
        }
        if (trimmedContent.isEmpty()) {
            throw new BusinessException("文章内容不能为空");
        }
        if (summary == null || summary.isBlank()) {
            summary = trimmedContent.length() > 100 ? trimmedContent.substring(0, 100) + "…" : trimmedContent;
        }
        Article article = new Article();
        article.setUserId(authorId);
        article.setTitle(trimmedTitle);
        article.setSummary(summary.trim());
        article.setContent(trimmedContent);
        LocalDateTime now = LocalDateTime.now();
        article.setCreateTime(now);
        article.setUpdateTime(now);
        articleMapper.insert(article);
        return article;
    }

    /** 编辑文章(仅作者) */
    public void update(Long id, Long operatorId, String title, String summary, String content) {
        Article article = getById(id);
        checkOwnership(article, operatorId);
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedContent = content == null ? "" : content.trim();
        if (trimmedTitle.isEmpty()) {
            throw new BusinessException("标题不能为空");
        }
        if (trimmedTitle.length() > 200) {
            throw new BusinessException("标题不能超过 200 字");
        }
        if (trimmedContent.isEmpty()) {
            throw new BusinessException("文章内容不能为空");
        }
        article.setTitle(trimmedTitle);
        article.setSummary(summary == null || summary.isBlank() ? "" : summary.trim());
        article.setContent(trimmedContent);
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    /** 删除文章(仅作者), 先级联删除其评论 */
    public void delete(Long id, Long operatorId) {
        Article article = getById(id);
        checkOwnership(article, operatorId);
        commentService.deleteByArticleId(id);
        articleMapper.deleteById(id);
    }

    /** 填充单篇文章作者昵称 */
    public void fillAuthor(Article article) {
        User author = userService.getById(article.getUserId());
        article.setAuthorName(author == null ? "未知用户"
                : (author.getNickname() == null ? author.getUsername() : author.getNickname()));
    }

    /** 批量填充作者昵称, 避免 N+1 查询 */
    private void fillAuthors(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        Set<Long> userIds = articles.stream()
                .map(Article::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() == null ? u.getUsername() : u.getNickname()));
        articles.forEach(a -> a.setAuthorName(nameMap.getOrDefault(a.getUserId(), "未知用户")));
    }

    /** 批量填充评论数 */
    private void fillCommentCounts(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        Set<Long> articleIds = articles.stream()
                .map(Article::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Long> counts = commentService.countGroupByArticleIds(articleIds);
        articles.forEach(a -> a.setCommentCount(counts.getOrDefault(a.getId(), 0L)));
    }

    /** 作者权限校验 */
    private void checkOwnership(Article article, Long operatorId) {
        if (article == null || operatorId == null || !article.getUserId().equals(operatorId)) {
            throw new BusinessException(403, "无权操作该文章");
        }
    }
}
