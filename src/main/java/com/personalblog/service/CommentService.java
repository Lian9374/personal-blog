package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Comment;
import com.personalblog.entity.User;
import com.personalblog.mapper.ArticleMapper;
import com.personalblog.mapper.CommentMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论业务。注意: 这里只依赖 ArticleMapper(而非 ArticleService)做文章存在性校验,
 * 避免与 ArticleService 产生循环依赖。
 */
@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final UserService userService;

    public CommentService(CommentMapper commentMapper, ArticleMapper articleMapper, UserService userService) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
        this.userService = userService;
    }

    /** 按文章查询评论(按时间正序) */
    public List<Comment> listByArticle(Long articleId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, articleId)
                        .orderByAsc(Comment::getCreateTime));
        fillAuthors(comments);
        return comments;
    }

    /** 发表评论 */
    public void add(Long articleId, Long userId, String content) {
        if (articleMapper.selectById(articleId) == null) {
            throw new BusinessException(404, "文章不存在");
        }
        content = content == null ? "" : content.trim();
        if (content.isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("评论内容不能超过 500 字");
        }
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);
    }

    /** 删除某文章下的全部评论(级联删除用) */
    public void deleteByArticleId(Long articleId) {
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getArticleId, articleId));
    }

    /** 批量统计各文章评论数, 返回 articleId -> 评论数 */
    public Map<Long, Long> countGroupByArticleIds(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = commentMapper.selectMaps(
                new QueryWrapper<Comment>()
                        .select("article_id", "COUNT(*) AS cnt")
                        .in("article_id", articleIds)
                        .groupBy("article_id"));
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long articleId = ((Number) row.get("article_id")).longValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            result.put(articleId, cnt);
        }
        return result;
    }

    /** 批量填充评论人昵称, 避免 N+1 查询 */
    private void fillAuthors(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() == null ? u.getUsername() : u.getNickname()));
        comments.forEach(c -> c.setAuthorName(nameMap.getOrDefault(c.getUserId(), "未知用户")));
    }
}
