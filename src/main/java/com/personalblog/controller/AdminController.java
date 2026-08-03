package com.personalblog.controller;

import com.personalblog.service.ArticleService;
import com.personalblog.service.BoardService;
import com.personalblog.service.CommentService;
import com.personalblog.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 管理后台: 仪表盘 + 用户管理
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ArticleService articleService;
    private final CommentService commentService;
    private final BoardService boardService;

    public AdminController(UserService userService, ArticleService articleService,
                           CommentService commentService, BoardService boardService) {
        this.userService = userService;
        this.articleService = articleService;
        this.commentService = commentService;
        this.boardService = boardService;
    }

    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("userCount", userService.count());
        model.addAttribute("postCount", articleService.count());
        model.addAttribute("commentCount", commentService.count());
        model.addAttribute("boardCount", boardService.count());
        model.addAttribute("latestUsers", userService.latestUsers(5));
        model.addAttribute("latestPosts", articleService.pageAll(1).getRecords());
        return "admin/index";
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "1") long page,
                        @RequestParam(required = false) String keyword,
                        Model model) {
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("page", userService.pageUsers(page, keyword));
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateRole(id, role);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        userService.updateStatus(id, status);
        return "redirect:/admin/users";
    }
}
