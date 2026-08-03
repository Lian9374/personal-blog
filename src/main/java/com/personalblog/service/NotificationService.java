package com.personalblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personalblog.entity.Article;
import com.personalblog.entity.Notification;
import com.personalblog.entity.User;
import com.personalblog.mapper.ArticleMapper;
import com.personalblog.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 站内通知
 */
@Service
public class NotificationService {

    private static final int PAGE_SIZE = 20;

    private final NotificationMapper notificationMapper;
    private final UserService userService;
    private final ArticleMapper articleMapper;

    public NotificationService(NotificationMapper notificationMapper, UserService userService, ArticleMapper articleMapper) {
        this.notificationMapper = notificationMapper;
        this.userService = userService;
        this.articleMapper = articleMapper;
    }

    /** 生成通知; recipient == actor 时跳过(不通知自己) */
    public void notify(Long recipientId, Long actorId, String type, Long articleId, Long commentId) {
        if (recipientId == null || actorId == null || recipientId.equals(actorId)) {
            return;
        }
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setActorId(actorId);
        n.setType(type);
        n.setArticleId(articleId);
        n.setCommentId(commentId);
        n.setIsRead(false);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /** 未读数 */
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getIsRead, false));
    }

    /** 分页查询(按时间倒序), 填充触发者昵称/头像/相关文章标题 */
    public IPage<Notification> pageByUser(Long userId, long current) {
        IPage<Notification> page = notificationMapper.selectPage(
                new Page<>(Math.max(current, 1), PAGE_SIZE),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getRecipientId, userId)
                        .orderByDesc(Notification::getCreateTime));
        fillDisplay(page.getRecords());
        return page;
    }

    /** 全部标记已读 */
    public void markAllRead(Long userId) {
        Notification update = new Notification();
        update.setIsRead(true);
        notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getIsRead, false));
    }

    /** 单条标记已读(校验归属) */
    public void markRead(Long userId, Long id) {
        Notification update = new Notification();
        update.setIsRead(true);
        notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getRecipientId, userId));
    }

    private void fillDisplay(List<Notification> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> actorIds = list.stream().map(Notification::getActorId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(actorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Set<Long> articleIds = list.stream().map(Notification::getArticleId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> titleMap = new HashMap<>();
        if (!articleIds.isEmpty()) {
            articleMapper.selectBatchIds(articleIds).forEach(a -> titleMap.put(a.getId(), a.getTitle()));
        }
        for (Notification n : list) {
            User actor = userMap.get(n.getActorId());
            if (actor != null) {
                n.setActorName(actor.getNickname() == null ? actor.getUsername() : actor.getNickname());
                n.setActorAvatar(actor.getAvatar());
            }
            n.setArticleTitle(titleMap.get(n.getArticleId()));
            n.setTypeText(typeText(n.getType()));
        }
    }

    private String typeText(String type) {
        if (type == null) {
            return "系统通知";
        }
        return switch (type) {
            case "REPLY" -> "回复了你";
            case "LIKE" -> "赞了你的帖子";
            case "FAVORITE" -> "收藏了你的帖子";
            case "FOLLOW" -> "关注了你";
            default -> "系统通知";
        };
    }

    public void deleteByArticleId(Long articleId) {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>().eq(Notification::getArticleId, articleId));
    }
}
