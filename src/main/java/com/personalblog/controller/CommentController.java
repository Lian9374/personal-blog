package com.personalblog.controller;

import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.User;
import com.personalblog.service.CommentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 评论: 发表评论
 */
@Controller
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/comment/add")
    public String add(@SessionAttribute("loginUser") User loginUser,
                      @RequestParam Long articleId,
                      @RequestParam String content,
                      RedirectAttributes ra) {
        try {
            commentService.add(articleId, loginUser.getId(), content);
            ra.addFlashAttribute("success", "评论成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/article/" + articleId;
    }
}
