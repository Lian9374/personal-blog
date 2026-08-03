package com.personalblog.interceptor;

import com.personalblog.common.exception.BusinessException;
import com.personalblog.entity.User;
import com.personalblog.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员拦截器: 仅注册于 /admin/**。每次读库取最新角色, 角色变更即时生效。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public AdminInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        User loginUser = session != null ? (User) session.getAttribute("loginUser") : null;
        if (loginUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        User fresh = userService.getById(loginUser.getId());
        if (fresh == null || !"ADMIN".equals(fresh.getRole())) {
            throw new BusinessException(403, "无权访问管理后台");
        }
        fresh.setPassword(null);
        session.setAttribute("loginUser", fresh);
        return true;
    }
}
