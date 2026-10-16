package com.campus.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trading.common.BusinessException;
import com.campus.trading.dto.LoginDTO;
import com.campus.trading.dto.RegisterDTO;
import com.campus.trading.entity.User;
import com.campus.trading.mapper.UserMapper;
import com.campus.trading.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务: 注册 / 登录
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == User.STATUS_DISABLED) {
            throw new BusinessException(403, "账号已被禁用, 请联系管理员");
        }
        String token = jwtUtil.createToken(user.getId(), user.getRole());
        user.setPassword(null);
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("user", user);
        return map;
    }

    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setRole(User.ROLE_STUDENT);
        user.setStatus(User.STATUS_NORMAL);
        userMapper.insert(user);
    }
}
