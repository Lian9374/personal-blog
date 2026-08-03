package com.personalblog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.Article;
import com.personalblog.entity.User;
import com.personalblog.service.ArticleService;
import com.personalblog.service.FavoriteService;
import com.personalblog.service.FollowService;
import com.personalblog.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * 用户: 注册 / 登录 / 登出 / 个人中心(含头像) / 公开主页 / 关注
 */
@Controller
public class UserController {

    private final UserService userService;
    private final FollowService followService;
    private final ArticleService articleService;
    private final FavoriteService favoriteService;

    public UserController(UserService userService, FollowService followService, ArticleService articleService,
                          FavoriteService favoriteService) {
        this.userService = userService;
        this.followService = followService;
        this.articleService = articleService;
        this.favoriteService = favoriteService;
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
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String newPassword,
                                Model model,
                                HttpSession session,
                                RedirectAttributes ra) {
        try {
            userService.updateProfile(loginUser.getId(), nickname, email, bio, newPassword);
            User fresh = userService.getById(loginUser.getId());
            fresh.setPassword(null);
            session.setAttribute("loginUser", fresh); // 刷新会话中的用户信息
            ra.addFlashAttribute("success", "资料修改成功");
            return "redirect:/profile";
        } catch (BusinessException e) {
            User user = userService.getById(loginUser.getId());
            user.setNickname(nickname);
            user.setEmail(email);
            user.setBio(bio);
            model.addAttribute("user", user);
            model.addAttribute("error", e.getMessage());
            return "profile";
        }
    }

    @PostMapping("/profile/avatar")
    public String updateAvatar(@SessionAttribute("loginUser") User loginUser,
                               @RequestParam("avatar") MultipartFile avatar,
                               HttpSession session,
                               RedirectAttributes ra,
                               Model model) {
        try {
            userService.updateAvatar(loginUser.getId(), avatar.getBytes());
            User fresh = userService.getById(loginUser.getId());
            fresh.setPassword(null);
            session.setAttribute("loginUser", fresh);
            ra.addFlashAttribute("success", "头像更新成功");
            return "redirect:/profile";
        } catch (BusinessException e) {
            model.addAttribute("user", userService.getById(loginUser.getId()));
            model.addAttribute("error", e.getMessage());
            return "profile";
        } catch (IOException e) {
            model.addAttribute("user", userService.getById(loginUser.getId()));
            model.addAttribute("error", "头像上传失败，请重试");
            return "profile";
        }
    }

    /** 公开个人主页(帖子/收藏 Tab) */
    @GetMapping("/user/{id}")
    public String publicProfile(@PathVariable Long id,
                                @RequestParam(defaultValue = "posts") String tab,
                                @SessionAttribute(name = "loginUser", required = false) User loginUser,
                                @RequestParam(defaultValue = "1") long page,
                                Model model) {
        User user = userService.getProfile(id);
        if (loginUser != null) {
            user.setFollowedByCurrentUser(followService.isFollowing(loginUser.getId(), id));
        } else {
            user.setFollowedByCurrentUser(false);
        }
        model.addAttribute("profileUser", user);
        model.addAttribute("tab", "favorites".equals(tab) ? "favorites" : "posts");
        model.addAttribute("page", "favorites".equals(tab)
                ? favoriteService.pageFavoritesByUser(id, page)
                : articleService.pageByUser(id, page));
        return "user/profile";
    }

    @PostMapping("/user/{id}/follow")
    public String follow(@PathVariable Long id, @SessionAttribute("loginUser") User loginUser) {
        followService.toggle(id, loginUser.getId());
        return "redirect:/user/" + id;
    }
}
