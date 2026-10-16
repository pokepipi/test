package com.campus.trading.controller;

import com.campus.trading.common.Result;
import com.campus.trading.config.JwtInterceptor;
import com.campus.trading.dto.LoginDTO;
import com.campus.trading.dto.RegisterDTO;
import com.campus.trading.entity.User;
import com.campus.trading.service.AuthService;
import com.campus.trading.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 认证接口: 注册 / 登录 / 当前用户
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public Result<User> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(userService.getById(userId));
    }
}
