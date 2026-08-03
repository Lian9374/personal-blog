package com.personalblog.controller;

import com.personalblog.service.ArticleService;
import com.personalblog.service.BoardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 版块: 首页版块导航 / 版块帖子流
 */
@Controller
public class BoardController {

    private final BoardService boardService;
    private final ArticleService articleService;

    public BoardController(BoardService boardService, ArticleService articleService) {
        this.boardService = boardService;
        this.articleService = articleService;
    }

    /** 论坛首页: 版块卡片 + 最新帖子 */
    @GetMapping("/")
    public String index(Model model) {
        // 覆盖 @ControllerAdvice 提供的无统计版块列表, 带上文章数/最后发帖时间
        model.addAttribute("boards", boardService.listAllWithCounts());
        model.addAttribute("latest", articleService.pageByBoard(null, 1, false).getRecords());
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
