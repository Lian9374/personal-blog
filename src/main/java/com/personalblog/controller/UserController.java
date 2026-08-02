package com.personalblog.controller;

import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.User;
import com.personalblog.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 用户: 注册 / 登录 / 登出 / 修改资料
 */
@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage(@SessionAttribute(name = "loginUser", required = false) User loginUser) {
        if (loginUser != null) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam(required = false) String nickname,
                           @RequestParam(required = false) String email,
                           Model model,
                           RedirectAttributes ra) {
        try {
            userService.register(username, password, nickname, email);
            ra.addFlashAttribute("success", "注册成功，请登录");
            return "redirect:/login";
        } catch (BusinessException e) {
            model.addAttribute("username", username);
            model.addAttribute("nickname", nickname);
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginPage(@SessionAttribute(name = "loginUser", required = false) User loginUser) {
        if (loginUser != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        Model model,
                        HttpSession session) {
        try {
            User user = userService.login(username, password);
            user.setPassword(null); // 不把密码密文放进会话
            session.setAttribute("loginUser", user);
            return "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("username", username);
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/profile")
    public String profilePage(@SessionAttribute("loginUser") User loginUser, Model model) {
        model.addAttribute("user", userService.getById(loginUser.getId()));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@SessionAttribute("loginUser") User loginUser,
                                @RequestParam(required = false) String nickname,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String newPassword,
                                Model model,
                                HttpSession session,
                                RedirectAttributes ra) {
        try {
            userService.updateProfile(loginUser.getId(), nickname, email, newPassword);
            User fresh = userService.getById(loginUser.getId());
            fresh.setPassword(null);
            session.setAttribute("loginUser", fresh); // 刷新会话中的用户信息
            ra.addFlashAttribute("success", "资料修改成功");
            return "redirect:/profile";
        } catch (BusinessException e) {
            User user = userService.getById(loginUser.getId());
            user.setNickname(nickname);
            user.setEmail(email);
            model.addAttribute("user", user);
            model.addAttribute("error", e.getMessage());
            return "profile";
        }
    }
}
