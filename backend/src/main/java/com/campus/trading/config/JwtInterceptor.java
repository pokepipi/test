package com.campus.trading.config;

import com.campus.trading.common.Result;
import com.campus.trading.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 登录拦截器
 *  - 所有 /api/** 请求需携带 Bearer Token
 *  - GET 公开接口(物品分页/详情/分类树)放行
 *  - /api/admin/** 要求 ADMIN 角色
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ROLE = "role";

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 公开 GET 接口: 物品分页 / 物品详情 / 分类树
        boolean publicGet = "GET".equals(method)
                && (uri.equals("/api/item/page")
                || uri.matches("/api/item/\\d+")
                || uri.equals("/api/category/tree"));
        // 公开 POST 接口: 登录 / 注册 (无需 token)
        boolean publicPost = "POST".equals(method)
                && (uri.equals("/api/auth/login") || uri.equals("/api/auth/register"));
        if (publicGet || publicPost) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
        if (token == null || !jwtUtil.validate(token)) {
            writeResult(response, 401, "未登录或登录已过期");
            return false;
        }

        Long userId = jwtUtil.parseUserId(token);
        String role = jwtUtil.parseRole(token);
        request.setAttribute(ATTR_USER_ID, userId);
        request.setAttribute(ATTR_ROLE, role);

        if (uri.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            writeResult(response, 403, "无管理员权限");
            return false;
        }
        return true;
    }

    private void writeResult(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
