package com.personalblog.controller;

import com.personalblog.service.ArticleService;
import com.personalblog.service.BoardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 版块: 首页内容流 / 版块帖子流
 */
@Controller
public class BoardController {

    private final BoardService boardService;
    private final ArticleService articleService;

    public BoardController(BoardService boardService, ArticleService articleService) {
        this.boardService = boardService;
        this.articleService = articleService;
    }

    /** 论坛首页: Hero + 最新帖子内容流(分页); 版块/统计由 GlobalModelAdvice 提供 */
    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "1") long page, Model model) {
        model.addAttribute("page", articleService.pageByBoard(null, page, false));
        return "index";
    }

    /** 版块帖子流: 置顶优先 + 时间倒序, 支持精华筛选 */
    @GetMapping("/board/{id}")
    public String board(@PathVariable Long id,
                        @RequestParam(defaultValue = "1") long page,
                        @RequestParam(required = false) Boolean essence,
                        Model model) {
        model.addAttribute("board", boardService.getById(id));
        model.addAttribute("page", articleService.pageByBoard(id, page, Boolean.TRUE.equals(essence)));
        return "board/detail";
    }
}
