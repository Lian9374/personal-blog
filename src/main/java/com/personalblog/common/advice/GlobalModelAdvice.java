package com.personalblog.common.advice;

import com.personalblog.entity.Article;
import com.personalblog.entity.Board;
import com.personalblog.entity.Tag;
import com.personalblog.entity.User;
import com.personalblog.service.ArticleService;
import com.personalblog.service.BoardService;
import com.personalblog.service.CommentService;
import com.personalblog.service.NotificationService;
import com.personalblog.service.TagService;
import com.personalblog.service.UserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局模型属性: 全站导航统一取数(左栏版块导航 + 右栏发现数据 + 未读通知数)。
 * 仅作用于业务控制器; 错误页由 BasicErrorController 渲染, 不在此范围。
 */
@ControllerAdvice(basePackages = "com.personalblog.controller")
public class GlobalModelAdvice {

    private final BoardService boardService;
    private final NotificationService notificationService;
    private final ArticleService articleService;
    private final TagService tagService;
    private final UserService userService;
    private final CommentService commentService;

    public GlobalModelAdvice(BoardService boardService, NotificationService notificationService,
                             ArticleService articleService, TagService tagService,
                             UserService userService, CommentService commentService) {
        this.boardService = boardService;
        this.notificationService = notificationService;
        this.articleService = articleService;
        this.tagService = tagService;
        this.userService = userService;
        this.commentService = commentService;
    }

    /** 左栏版块导航 + 导航下拉(带帖子数) */
    @ModelAttribute("boards")
    public List<Board> boards() {
        return boardService.listAllWithCounts();
    }

    /** 全站导航未读通知数 */
    @ModelAttribute("unreadCount")
    public long unreadCount(@SessionAttribute(name = "loginUser", required = false) User loginUser) {
        if (loginUser == null) {
            return 0;
        }
        return notificationService.unreadCount(loginUser.getId());
    }

    /** 右栏: 热门帖子 */
    @ModelAttribute("hotPosts")
    public List<Article> hotPosts() {
        return articleService.hotPosts(5);
    }

    /** 右栏: 标签云 */
    @ModelAttribute("topTags")
    public List<Tag> topTags() {
        return tagService.listAllWithCounts(10);
    }

    /** 右栏: 活跃成员 */
    @ModelAttribute("topUsers")
    public List<User> topUsers() {
        return userService.topUsers(5);
    }

    /** 右栏: 社区统计 */
    @ModelAttribute("stats")
    public Map<String, Long> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("userCount", userService.count());
        stats.put("postCount", articleService.count());
        stats.put("commentCount", commentService.count());
        stats.put("boardCount", boardService.count());
        return stats;
    }
}
