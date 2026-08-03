package com.personalblog.controller;

import com.personalblog.entity.User;
import com.personalblog.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * 站内通知: 列表 / 已读
 */
@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String list(@SessionAttribute("loginUser") User loginUser,
                       @RequestParam(defaultValue = "1") long page,
                       Model model) {
        model.addAttribute("page", notificationService.pageByUser(loginUser.getId(), page));
        return "notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllRead(@SessionAttribute("loginUser") User loginUser) {
        notificationService.markAllRead(loginUser.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/mark-read")
    public String markRead(@SessionAttribute("loginUser") User loginUser, @RequestParam Long id) {
        notificationService.markRead(loginUser.getId(), id);
        return "redirect:/notifications";
    }
}
