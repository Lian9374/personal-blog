package com.personalblog.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器(fail-closed): 会话中无 loginUser 且非公开路径则重定向到登录页。
 * 公开路径分"始终公开"与"仅 GET 公开"两档, 避免 POST 写操作被放行。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        boolean loggedIn = session != null && session.getAttribute("loginUser") != null;
        if (loggedIn || isPublic(request.getMethod(), path(request))) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private boolean isPublic(String method, String path) {
        // 始终公开: 账号页 + 静态资源
        if (path.equals("/login") || path.equals("/register") || path.equals("/logout")
                || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.startsWith("/uploads/") || path.equals("/favicon.ico") || path.equals("/error")) {
            return true;
        }
        // 仅 GET 公开: 浏览类页面(POST 到同一前缀的写操作仍受保护)
        if ("GET".equals(method)) {
            return path.equals("/")
                    || path.equals("/article/list")
                    || path.startsWith("/board/")
                    || path.startsWith("/tag/")
                    || path.equals("/search")
                    || path.startsWith("/user/")
                    || path.matches("/article/\\d+");
        }
        return false;
    }
}
