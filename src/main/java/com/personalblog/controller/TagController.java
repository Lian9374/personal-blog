package com.personalblog.controller;

import com.personalblog.service.ArticleService;
import com.personalblog.service.TagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 标签: 标签下的帖子流
 */
@Controller
public class TagController {

    private final TagService tagService;
    private final ArticleService articleService;

    public TagController(TagService tagService, ArticleService articleService) {
        this.tagService = tagService;
        this.articleService = articleService;
    }

    @GetMapping("/tag/{name}")
    public String tagPage(@PathVariable String name,
                          @RequestParam(defaultValue = "1") long page,
                          Model model) {
        model.addAttribute("tag", tagService.getByNameOr404(name));
        model.addAttribute("page", articleService.pageByTag(tagService.getByNameOr404(name).getId(), page));
        return "tag/detail";
    }
}
