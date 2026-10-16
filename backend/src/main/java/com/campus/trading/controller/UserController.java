package com.campus.trading.controller;

import com.campus.trading.common.Result;
import com.campus.trading.config.JwtInterceptor;
import com.campus.trading.dto.PasswordDTO;
import com.campus.trading.dto.ProfileDTO;
import com.campus.trading.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户接口: 个人资料 / 修改密码
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody ProfileDTO dto, HttpServletRequest request) {
        userService.updateProfile((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto);
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Validated PasswordDTO dto, HttpServletRequest request) {
        userService.changePassword((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto);
        return Result.ok();
    }
}
