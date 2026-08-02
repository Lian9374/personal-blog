package com.personalblog.config;

import com.personalblog.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置: 注册登录拦截器(采用保护路径白名单, 公开路径不会被拦截)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(
                        "/profile", "/profile/**",
                        "/article/create",
                        "/article/*/edit",
                        "/article/*/delete",
                        "/comment/add")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/favicon.ico", "/error");
    }
}
