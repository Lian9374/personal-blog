package com.personalblog.common.advice;

import com.personalblog.entity.Board;
import com.personalblog.entity.User;
import com.personalblog.service.BoardService;
import com.personalblog.service.NotificationService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

/**
 * 全局模型属性: 全站导航统一取数(版块下拉 + 未读通知数)。
 * 仅作用于业务控制器; 错误页由 BasicErrorController 渲染, 不在此范围。
 */
@ControllerAdvice(basePackages = "com.personalblog.controller")
public class GlobalModelAdvice {

    private final BoardService boardService;
    private final NotificationService notificationService;

    public GlobalModelAdvice(BoardService boardService, NotificationService notificationService) {
        this.boardService = boardService;
        this.notificationService = notificationService;
    }

    /** 全站导航版块下拉 */
    @ModelAttribute("boards")
    public List<Board> boards() {
        return boardService.listAll();
    }

    /** 全站导航未读通知数 */
    @ModelAttribute("unreadCount")
    public long unreadCount(@SessionAttribute(name = "loginUser", required = false) User loginUser) {
        if (loginUser == null) {
            return 0;
        }
        return notificationService.unreadCount(loginUser.getId());
    }
}
