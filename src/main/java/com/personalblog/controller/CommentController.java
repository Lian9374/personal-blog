package com.personalblog.controller;

import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.User;
import com.personalblog.service.CommentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 评论: 发表/回复 / 删除
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
                      @RequestParam(required = false) Long parentId,
                      @RequestParam String content,
                      RedirectAttributes ra) {
        try {
            commentService.add(articleId, loginUser.getId(), content, parentId);
            ra.addFlashAttribute("success", "评论成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/article/" + articleId;
    }

    @PostMapping("/comment/{id}/delete")
    public String delete(@PathVariable Long id,
                         @SessionAttribute("loginUser") User loginUser,
                         RedirectAttributes ra) {
        Long articleId = commentService.delete(id, loginUser.getId());
        ra.addFlashAttribute("success", "评论删除成功");
        return "redirect:/article/" + articleId;
    }
}
