package com.personalblog.controller;

import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.Comment;
import com.personalblog.entity.User;
import com.personalblog.service.ArticleService;
import com.personalblog.service.CommentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 文章: 列表 / 详情 / 发布 / 编辑 / 删除
 */
@Controller
public class ArticleController {

    private final ArticleService articleService;
    private final CommentService commentService;

    public ArticleController(ArticleService articleService, CommentService commentService) {
        this.articleService = articleService;
        this.commentService = commentService;
    }

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "1") long page, Model model) {
        model.addAttribute("page", articleService.pageArticles(page));
        return "index";
    }

    @GetMapping("/article/list")
    public String list() {
        return "redirect:/";
    }

    @GetMapping("/article/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Article article = articleService.getById(id);
        List<Comment> comments = commentService.listByArticle(id);
        model.addAttribute("article", article);
        model.addAttribute("comments", comments);
        return "article/detail";
    }

    @GetMapping("/article/create")
    public String createPage(Model model) {
        model.addAttribute("article", new Article());
        model.addAttribute("mode", "create");
        return "article/form";
    }

    @PostMapping("/article/create")
    public String create(@SessionAttribute("loginUser") User loginUser,
                         @RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam String content,
                         Model model,
                         RedirectAttributes ra) {
        try {
            Article article = articleService.create(loginUser.getId(), title, summary, content);
            ra.addFlashAttribute("success", "文章发布成功");
            return "redirect:/article/" + article.getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("mode", "create");
            Article a = new Article();
            a.setTitle(title);
            a.setSummary(summary);
            a.setContent(content);
            model.addAttribute("article", a);
            return "article/form";
        }
    }

    @GetMapping("/article/{id}/edit")
    public String editPage(@PathVariable Long id,
                           @SessionAttribute("loginUser") User loginUser,
                           Model model) {
        Article article = articleService.getById(id);
        if (!article.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(403, "无权操作该文章");
        }
        model.addAttribute("article", article);
        model.addAttribute("mode", "edit");
        return "article/form";
    }

    @PostMapping("/article/{id}/edit")
    public String update(@PathVariable Long id,
                         @SessionAttribute("loginUser") User loginUser,
                         @RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam String content,
                         Model model,
                         RedirectAttributes ra) {
        try {
            articleService.update(id, loginUser.getId(), title, summary, content);
            ra.addFlashAttribute("success", "文章修改成功");
            return "redirect:/article/" + id;
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("mode", "edit");
            Article a = articleService.getById(id);
            a.setTitle(title);
            a.setSummary(summary);
            a.setContent(content);
            model.addAttribute("article", a);
            return "article/form";
        }
    }

    @PostMapping("/article/{id}/delete")
    public String delete(@PathVariable Long id,
                         @SessionAttribute("loginUser") User loginUser,
                         RedirectAttributes ra) {
        articleService.delete(id, loginUser.getId());
        ra.addFlashAttribute("success", "文章删除成功");
        return "redirect:/";
    }
}
