package com.personalblog.common.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理: 渲染 error.html 并设置真实 HTTP 状态码。
 * 注意: 视图渲染场景用 @ControllerAdvice(而非 @RestControllerAdvice)。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, Model model, HttpServletResponse response) {
        int status = (e.getCode() >= 400 && e.getCode() < 600) ? e.getCode() : 400;
        response.setStatus(status);
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model, HttpServletResponse response) {
        log.error("系统异常", e);
        response.setStatus(500);
        model.addAttribute("message", "系统繁忙，请稍后再试");
        return "error";
    }
}
