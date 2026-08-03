package com.personalblog.controller;

import com.personalblog.service.ArticleService;
import com.personalblog.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 搜索: 帖子 + 用户
 */
@Controller
public class SearchController {

    private final ArticleService articleService;
    private final UserService userService;

    public SearchController(ArticleService articleService, UserService userService) {
        this.articleService = articleService;
        this.userService = userService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "1") long page,
                         @RequestParam(required = false) Long boardId,
                         Model model) {
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("posts", articleService.search(q, page, boardId));
        model.addAttribute("users", userService.searchUser(q));
        return "search";
    }
}
