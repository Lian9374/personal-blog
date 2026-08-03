package com.personalblog.controller;

import com.personalblog.common.exception.BusinessException;
import com.personalblog.service.BoardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 管理后台: 版块管理
 */
@Controller
@RequestMapping("/admin/boards")
public class AdminBoardController {

    private final BoardService boardService;

    public AdminBoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("")
    public String boards(Model model) {
        model.addAttribute("boards", boardService.listAllWithCounts());
        return "admin/boards";
    }

    @PostMapping("/create")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "0") int sortOrder,
                         RedirectAttributes ra) {
        try {
            boardService.create(name, description, sortOrder);
            ra.addFlashAttribute("success", "版块创建成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/boards";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) String description,
                       @RequestParam(defaultValue = "0") int sortOrder,
                       RedirectAttributes ra) {
        try {
            boardService.update(id, name, description, sortOrder);
            ra.addFlashAttribute("success", "版块修改成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/boards";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            boardService.delete(id);
            ra.addFlashAttribute("success", "版块删除成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/boards";
    }
}
