package com.personalblog.controller;

import com.personalblog.entity.User;
import com.personalblog.service.CommentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 管理后台: 评论管理
 */
@Controller
@RequestMapping("/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("")
    public String comments(@RequestParam(defaultValue = "1") long page,
                           @RequestParam(required = false) Long articleId,
                           Model model) {
        model.addAttribute("page", commentService.pageAll(page, articleId));
        return "admin/comments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @SessionAttribute("loginUser") User loginUser,
                         RedirectAttributes ra) {
        commentService.delete(id, loginUser.getId());
        ra.addFlashAttribute("success", "评论已删除");
        return "redirect:/admin/comments";
    }
}
