package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.ArticleLike;
import com.personalblog.entity.ArticleTag;
import com.personalblog.entity.Board;
import com.personalblog.entity.Favorite;
import com.personalblog.entity.Notification;
import com.personalblog.entity.Tag;
import com.personalblog.entity.User;
import com.personalblog.mapper.ArticleLikeMapper;
import com.personalblog.mapper.ArticleMapper;
import com.personalblog.mapper.ArticleTagMapper;
import com.personalblog.mapper.BoardMapper;
import com.personalblog.mapper.FavoriteMapper;
import com.personalblog.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章(帖子)业务
 */
@Service
public class ArticleService {

    private static final int PAGE_SIZE = 10;

    private final ArticleMapper articleMapper;
    private final CommentService commentService;
    private final UserService userService;
    private final TagService tagService;
    private final MarkdownService markdownService;
    private final BoardMapper boardMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final NotificationMapper notificationMapper;

    public ArticleService(ArticleMapper articleMapper, CommentService commentService, UserService userService,
                          TagService tagService, MarkdownService markdownService, BoardMapper boardMapper,
                          ArticleTagMapper articleTagMapper, ArticleLikeMapper articleLikeMapper,
                          FavoriteMapper favoriteMapper, NotificationMapper notificationMapper) {
        this.articleMapper = articleMapper;
        this.commentService = commentService;
        this.userService = userService;
        this.tagService = tagService;
        this.markdownService = markdownService;
        this.boardMapper = boardMapper;
        this.articleTagMapper = articleTagMapper;
        this.articleLikeMapper = articleLikeMapper;
        this.favoriteMapper = favoriteMapper;
        this.notificationMapper = notificationMapper;
    }

    // ==================== 查询 ====================

    /** 版块下帖子流: 置顶优先 + 时间倒序 */
    public IPage<Article> pageByBoard(Long boardId, long current, boolean essenceOnly) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<Article>()
                .eq(boardId != null, Article::getBoardId, boardId)
                .eq(essenceOnly, Article::getEssence, true)
                .orderByDesc(Article::getPinned)
                .orderByDesc(Article::getCreateTime);
        return page(qw, current);
    }

    /** 全站帖子(管理端) */
    public IPage<Article> pageAll(long current) {
        return page(new LambdaQueryWrapper<Article>()
                .orderByDesc(Article::getPinned)
                .orderByDesc(Article::getCreateTime), current);
    }

    /** 热门帖子(右栏发现): 按 赞+评论×2+浏览/10 综合热度排序, 取 Top n */
    public List<Article> hotPosts(int n) {
        List<Article> list = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .last("ORDER BY (like_count + comment_count * 2 + view_count / 10) DESC, create_time DESC LIMIT " + n));
        fillList(list);
        return list;
    }

    /** 某标签下的帖子流 */
    public IPage<Article> pageByTag(Long tagId, long current) {
        IPage<Article> result = new Page<>(Math.max(current, 1), PAGE_SIZE);
        List<ArticleTag> links = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tagId));
        if (links.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }
        List<Long> ids = links.stream().map(ArticleTag::getArticleId).distinct().toList();
        return page(new LambdaQueryWrapper<Article>()
                .in(Article::getId, ids)
                .orderByDesc(Article::getPinned)
                .orderByDesc(Article::getCreateTime), current);
    }

    /** 关键词搜索(标题/摘要/正文 ILIKE, 通配符已转义) */
    public IPage<Article> search(String keyword, long current, Long boardId) {
        String kw = keyword == null ? "" : keyword.trim();
        IPage<Article> result = new Page<>(Math.max(current, 1), PAGE_SIZE);
        if (kw.isEmpty()) {
            result.setRecords(List.of());
            return result;
        }
        String pattern = "%" + escapeLike(kw) + "%";
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<Article>()
                .eq(boardId != null, Article::getBoardId, boardId)
                .and(w -> w.apply("(title ILIKE {0} ESCAPE '\\' OR summary ILIKE {0} ESCAPE '\\' OR content ILIKE {0} ESCAPE '\\')", pattern))
                .orderByDesc(Article::getCreateTime);
        return page(qw, current);
    }

    /** 某用户的帖子(个人主页) */
    public IPage<Article> pageByUser(Long userId, long current) {
        return page(new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId)
                .orderByDesc(Article::getCreateTime), current);
    }

    /** 按 ID 批量查询并填充展示字段(用户收藏/点赞页用) */
    public List<Article> listByIdsForDisplay(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Article> list = articleMapper.selectBatchIds(ids);
        fillList(list);
        return list;
    }

    /** 详情: 浏览量+1, 填充作者/版块/标签/Markdown 渲染/当前用户点赞收藏态 */
    public Article getDetail(Long id, Long currentUserId) {
        articleMapper.incrementViewCount(id);
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        fillAuthor(article);
        article.setTags(tagService.listByArticle(id));
        article.setContentHtml(markdownService.renderToHtml(article.getContent()));
        if (currentUserId != null) {
            article.setLiked(articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, id)
                    .eq(ArticleLike::getUserId, currentUserId)) > 0);
            article.setFavorited(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getArticleId, id)
                    .eq(Favorite::getUserId, currentUserId)) > 0);
        } else {
            article.setLiked(false);
            article.setFavorited(false);
        }
        return article;
    }

    /** 文章总数(管理端) */
    public long count() {
        return articleMapper.selectCount(new LambdaQueryWrapper<>());
    }

    // ==================== 写操作 ====================

    public Article create(Long authorId, Long boardId, String title, String summary, String content, List<String> tagNames) {
        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();
        if (t.isEmpty()) {
            throw new BusinessException("标题不能为空");
        }
        if (t.length() > 200) {
            throw new BusinessException("标题不能超过 200 字");
        }
        if (c.isEmpty()) {
            throw new BusinessException("文章内容不能为空");
        }
        if (boardId == null || boardMapper.selectById(boardId) == null) {
            throw new BusinessException(404, "版块不存在");
        }
        String s = (summary == null || summary.isBlank())
                ? (c.length() > 100 ? c.substring(0, 100) + "…" : c)
                : summary.trim();
        Article a = new Article();
        a.setUserId(authorId);
        a.setBoardId(boardId);
        a.setTitle(t);
        a.setSummary(s);
        a.setContent(c);
        a.setViewCount(0L);
        a.setCommentCount(0L);
        a.setLikeCount(0L);
        a.setFavoriteCount(0L);
        a.setPinned(false);
        a.setEssence(false);
        LocalDateTime now = LocalDateTime.now();
        a.setCreateTime(now);
        a.setUpdateTime(now);
        articleMapper.insert(a);
        tagService.saveArticleTags(a.getId(), tagNames);
        return a;
    }

    public void update(Long id, Long operatorId, Long boardId, String title, String summary, String content, List<String> tagNames) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(404, "文章不存在");
        }
        checkOwnershipOrAdmin(a, operatorId);
        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();
        if (t.isEmpty()) {
            throw new BusinessException("标题不能为空");
        }
        if (t.length() > 200) {
            throw new BusinessException("标题不能超过 200 字");
        }
        if (c.isEmpty()) {
            throw new BusinessException("文章内容不能为空");
        }
        if (boardId == null || boardMapper.selectById(boardId) == null) {
            throw new BusinessException(404, "版块不存在");
        }
        a.setBoardId(boardId);
        a.setTitle(t);
        a.setSummary((summary == null || summary.isBlank()) ? "" : summary.trim());
        a.setContent(c);
        a.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(a);
        tagService.saveArticleTags(id, tagNames);
    }

    /** 删除帖子: 级联删评论/标签/点赞/收藏/通知(作者或管理员) */
    @Transactional
    public void delete(Long id, Long operatorId) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(404, "文章不存在");
        }
        checkOwnershipOrAdmin(a, operatorId);
        commentService.deleteByArticleId(id);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, id));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getArticleId, id));
        notificationMapper.delete(new LambdaQueryWrapper<Notification>().eq(Notification::getArticleId, id));
        articleMapper.deleteById(id);
    }

    /** 编辑页预填: 校验权限后返回完整文章(含标签), 不增加浏览量 */
    public Article getForEdit(Long id, Long operatorId) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(404, "文章不存在");
        }
        checkOwnershipOrAdmin(a, operatorId);
        a.setTags(tagService.listByArticle(id));
        return a;
    }

    public void togglePin(Long id) {
        ensureExists(id);
        articleMapper.togglePinned(id);
    }

    public void toggleEssence(Long id) {
        ensureExists(id);
        articleMapper.toggleEssence(id);
    }

    private void ensureExists(Long id) {
        if (articleMapper.selectById(id) == null) {
            throw new BusinessException(404, "文章不存在");
        }
    }

    // ==================== 展示填充 ====================

    /** 单篇文章填充作者/版块(详情用, 兼容旧调用) */
    public void fillAuthor(Article a) {
        if (a == null) {
            return;
        }
        User author = userService.getById(a.getUserId());
        if (author != null) {
            a.setAuthorName(author.getNickname() == null ? author.getUsername() : author.getNickname());
            a.setAuthorAvatar(author.getAvatar());
        } else {
            a.setAuthorName("未知用户");
        }
        a.setBoardName(boardName(a.getBoardId()));
    }

    public boolean isAdmin(Long operatorId) {
        if (operatorId == null) {
            return false;
        }
        User u = userService.getById(operatorId);
        return u != null && "ADMIN".equals(u.getRole());
    }

    private IPage<Article> page(LambdaQueryWrapper<Article> qw, long current) {
        // 列表投影排除 content 大字段
        IPage<Article> page = articleMapper.selectPage(
                new Page<>(Math.max(current, 1), PAGE_SIZE),
                qw.select(Article.class, f -> !"content".equals(f.getColumn())));
        fillList(page.getRecords());
        return page;
    }

    private void fillList(List<Article> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> userIds = list.stream().map(Article::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Set<Long> boardIds = list.stream().map(Article::getBoardId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Board> boardMap = boardIds.isEmpty() ? Map.of()
                : boardMapper.selectBatchIds(boardIds).stream().collect(Collectors.toMap(Board::getId, b -> b));
        Map<Long, List<Tag>> tagMap = tagService.listByArticleIds(list.stream().map(Article::getId).toList());
        for (Article a : list) {
            User author = userMap.get(a.getUserId());
            if (author != null) {
                a.setAuthorName(author.getNickname() == null ? author.getUsername() : author.getNickname());
                a.setAuthorAvatar(author.getAvatar());
            } else {
                a.setAuthorName("未知用户");
            }
            Board board = boardMap.get(a.getBoardId());
            a.setBoardName(board == null ? "未知版块" : board.getName());
            a.setTags(tagMap.getOrDefault(a.getId(), List.of()));
        }
    }

    private String boardName(Long boardId) {
        if (boardId == null) {
            return "未知版块";
        }
        Board b = boardMapper.selectById(boardId);
        return b == null ? "未知版块" : b.getName();
    }

    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void checkOwnershipOrAdmin(Article a, Long operatorId) {
        if (a == null || operatorId == null || !(a.getUserId().equals(operatorId) || isAdmin(operatorId))) {
            throw new BusinessException(403, "无权操作该文章");
        }
    }
}
