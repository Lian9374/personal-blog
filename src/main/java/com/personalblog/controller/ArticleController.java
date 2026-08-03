package com.personalblog.controller;

import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.Tag;
import com.personalblog.entity.User;
import com.personalblog.service.ArticleLikeService;
import com.personalblog.service.ArticleService;
import com.personalblog.service.CommentService;
import com.personalblog.service.FavoriteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * 文章(帖子): 列表 / 详情 / 发布 / 编辑 / 删除 / 点赞 / 收藏
 */
@Controller
public class ArticleController {

    private final ArticleService articleService;
    private final CommentService commentService;
    private final ArticleLikeService articleLikeService;
    private final FavoriteService favoriteService;

    public ArticleController(ArticleService articleService, CommentService commentService,
                             ArticleLikeService articleLikeService, FavoriteService favoriteService) {
        this.articleService = articleService;
        this.commentService = commentService;
        this.articleLikeService = articleLikeService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/article/list")
    public String list() {
        return "redirect:/";
    }

    @GetMapping("/article/{id}")
    public String detail(@PathVariable Long id,
                         @SessionAttribute(name = "loginUser", required = false) User loginUser,
                         Model model) {
        Long currentUserId = loginUser == null ? null : loginUser.getId();
        model.addAttribute("article", articleService.getDetail(id, currentUserId));
        model.addAttribute("comments", commentService.listTreeByArticle(id));
        return "article/detail";
    }

    @GetMapping("/article/create")
    public String createPage(Model model) {
        model.addAttribute("article", new Article());
        model.addAttribute("mode", "create");
        model.addAttribute("tags", "");
        return "article/form";
    }

    @PostMapping("/article/create")
    public String create(@SessionAttribute("loginUser") User loginUser,
                         @RequestParam Long boardId,
                         @RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam String content,
                         @RequestParam(required = false) String tags,
                         Model model,
                         RedirectAttributes ra) {
        try {
            Article article = articleService.create(loginUser.getId(), boardId, title, summary, content, parseTags(tags));
            ra.addFlashAttribute("success", "帖子发布成功");
            return "redirect:/article/" + article.getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("mode", "create");
            Article a = new Article();
            a.setBoardId(boardId);
            a.setTitle(title);
            a.setSummary(summary);
            a.setContent(content);
            model.addAttribute("tags", tags);
            model.addAttribute("article", a);
            return "article/form";
        }
    }

    @GetMapping("/article/{id}/edit")
    public String editPage(@PathVariable Long id,
                           @SessionAttribute("loginUser") User loginUser,
                           Model model) {
        Article article = articleService.getForEdit(id, loginUser.getId());
        model.addAttribute("article", article);
        model.addAttribute("mode", "edit");
        model.addAttribute("tags", article.getTags() == null ? ""
                : article.getTags().stream().map(Tag::getName).collect(java.util.stream.Collectors.joining(", ")));
        return "article/form";
    }

    @PostMapping("/article/{id}/edit")
    public String update(@PathVariable Long id,
                         @SessionAttribute("loginUser") User loginUser,
                         @RequestParam Long boardId,
                         @RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam String content,
                         @RequestParam(required = false) String tags,
                         Model model,
                         RedirectAttributes ra) {
        try {
            articleService.update(id, loginUser.getId(), boardId, title, summary, content, parseTags(tags));
            ra.addFlashAttribute("success", "帖子修改成功");
            return "redirect:/article/" + id;
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("mode", "edit");
            Article a = articleService.getForEdit(id, loginUser.getId());
            a.setBoardId(boardId);
            a.setTitle(title);
            a.setSummary(summary);
            a.setContent(content);
            model.addAttribute("tags", tags);
            model.addAttribute("article", a);
            return "article/form";
        }
    }

    @PostMapping("/article/{id}/delete")
    public String delete(@PathVariable Long id,
                         @SessionAttribute("loginUser") User loginUser,
                         RedirectAttributes ra) {
        articleService.delete(id, loginUser.getId());
        ra.addFlashAttribute("success", "帖子删除成功");
        return "redirect:/";
    }

    @PostMapping("/article/{id}/like")
    public String like(@PathVariable Long id, @SessionAttribute("loginUser") User loginUser) {
        articleLikeService.toggle(id, loginUser.getId());
        return "redirect:/article/" + id;
    }

    @PostMapping("/article/{id}/favorite")
    public String favorite(@PathVariable Long id, @SessionAttribute("loginUser") User loginUser) {
        favoriteService.toggle(id, loginUser.getId());
        return "redirect:/article/" + id;
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        // 仅按逗号(半角/全角/分号)分割, 不按空格分割, 避免 "Spring Boot" 被拆开
        return Arrays.stream(tags.split("[,，;；]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(10)
                .toList();
    }
}
