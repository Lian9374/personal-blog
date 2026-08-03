package com.personalblog.controller;

import com.personalblog.entity.User;
import com.personalblog.service.ArticleService;
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
 * 管理后台: 帖子管理(置顶/精华/删除)
 */
@Controller
@RequestMapping("/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("")
    public String articles(@RequestParam(defaultValue = "1") long page, Model model) {
        model.addAttribute("page", articleService.pageAll(page));
        return "admin/articles";
    }

    @PostMapping("/{id}/pin")
    public String pin(@PathVariable Long id) {
        articleService.togglePin(id);
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/essence")
    public String essence(@PathVariable Long id) {
        articleService.toggleEssence(id);
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @SessionAttribute("loginUser") User loginUser,
                         RedirectAttributes ra) {
        articleService.delete(id, loginUser.getId());
        ra.addFlashAttribute("success", "帖子已删除");
        return "redirect:/admin/articles";
    }
}
