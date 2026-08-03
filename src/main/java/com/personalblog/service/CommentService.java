package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.Comment;
import com.personalblog.entity.Notification;
import com.personalblog.entity.User;
import com.personalblog.mapper.ArticleMapper;
import com.personalblog.mapper.CommentMapper;
import com.personalblog.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论业务。只依赖 ArticleMapper(而非 ArticleService)做文章存在性校验, 避免循环依赖。
 */
@Service
public class CommentService {

    private static final int MAX_CONTENT = 1000;

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    public CommentService(CommentMapper commentMapper, ArticleMapper articleMapper, UserService userService,
                          NotificationService notificationService, NotificationMapper notificationMapper) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
        this.userService = userService;
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    /** 发表评论/回复: 顶层取楼层, 回复可嵌套; 通知被回复者或文章作者 */
    public Comment add(Long articleId, Long userId, String content, Long parentId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        content = content == null ? "" : content.trim();
        if (content.isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > MAX_CONTENT) {
            throw new BusinessException("评论内容不能超过 " + MAX_CONTENT + " 字");
        }
        Comment parent = null;
        if (parentId != null) {
            parent = commentMapper.selectById(parentId);
            if (parent == null || !articleId.equals(parent.getArticleId())) {
                throw new BusinessException("要回复的评论不存在");
            }
        }
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setFloor(commentMapper.nextFloor(articleId));
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        articleMapper.changeCommentCount(articleId, +1);

        Long recipient = parent != null ? parent.getUserId() : article.getUserId();
        notificationService.notify(recipient, userId, "REPLY", articleId, comment.getId());
        return comment;
    }

    /** 文章评论树: 顶层评论(按楼层) + 各自回复 */
    public List<Comment> listTreeByArticle(Long articleId) {
        List<Comment> all = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .orderByAsc(Comment::getFloor)
                .orderByAsc(Comment::getCreateTime));
        if (all.isEmpty()) {
            return List.of();
        }
        fillAuthors(all);
        Map<Long, Comment> map = all.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c, (x, y) -> x));
        for (Comment c : all) {
            c.setChildren(new ArrayList<>());
            if (c.getParentId() != null) {
                Comment parent = map.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(c);
                    c.setReplyToName(parent.getAuthorName());
                }
            }
        }
        return all.stream().filter(c -> c.getParentId() == null).collect(Collectors.toList());
    }

    /** 删除评论(评论作者/文章作者/管理员), 连同所有回复; 返回所属文章ID */
    @Transactional
    public Long delete(Long commentId, Long operatorId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        Article article = articleMapper.selectById(comment.getArticleId());
        boolean isArticleAuthor = article != null && article.getUserId().equals(operatorId);
        boolean isAdmin = operatorId != null && userService.getById(operatorId) != null
                && "ADMIN".equals(userService.getById(operatorId).getRole());
        if (comment.getUserId().equals(operatorId) || isArticleAuthor || isAdmin) {
            int deleted = deleteWithDescendants(comment.getId());
            articleMapper.changeCommentCount(comment.getArticleId(), -deleted);
            return comment.getArticleId();
        } else {
            throw new BusinessException(403, "无权删除该评论");
        }
    }

    /** 删除某文章下的全部评论(级联删除用), 同步扣减计数与通知 */
    public void deleteByArticleId(Long articleId) {
        List<Comment> list = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getArticleId, articleId));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getArticleId, articleId));
        notificationMapper.delete(new LambdaQueryWrapper<Notification>().eq(Notification::getArticleId, articleId));
        if (!list.isEmpty()) {
            articleMapper.changeCommentCount(articleId, -list.size());
        }
    }

    /** 某用户的评论数 */
    public long countByUser(Long userId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId));
    }

    /** 评论总数(管理端) */
    public long count() {
        return commentMapper.selectCount(new LambdaQueryWrapper<>());
    }

    /** 管理端评论分页 */
    public com.baomidou.mybatisplus.core.metadata.IPage<Comment> pageAll(long current, Long articleId) {
        com.baomidou.mybatisplus.core.metadata.IPage<Comment> page = commentMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(Math.max(current, 1), 20),
                new LambdaQueryWrapper<Comment>()
                        .eq(articleId != null, Comment::getArticleId, articleId)
                        .orderByDesc(Comment::getCreateTime));
        fillAuthors(page.getRecords());
        return page;
    }

    /** 递归收集评论及其所有回复的 ID */
    private int deleteWithDescendants(Long rootId) {
        List<Long> ids = new ArrayList<>();
        collectIds(rootId, ids);
        commentMapper.deleteBatchIds(ids);
        return ids.size();
    }

    private void collectIds(Long parentId, List<Long> acc) {
        acc.add(parentId);
        List<Comment> children = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, parentId));
        for (Comment child : children) {
            collectIds(child.getId(), acc);
        }
    }

    /** 批量填充评论人昵称/头像, 避免 N+1 */
    private void fillAuthors(List<Comment> comments) {
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        comments.forEach(c -> {
            User author = userMap.get(c.getUserId());
            if (author != null) {
                c.setAuthorName(author.getNickname() == null ? author.getUsername() : author.getNickname());
                c.setAuthorAvatar(author.getAvatar());
            } else {
                c.setAuthorName("未知用户");
            }
        });
    }
}
