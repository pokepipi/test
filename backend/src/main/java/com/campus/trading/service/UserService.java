package com.campus.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trading.common.BusinessException;
import com.campus.trading.dto.PasswordDTO;
import com.campus.trading.dto.ProfileDTO;
import com.campus.trading.entity.User;
import com.campus.trading.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务: 个人资料 / 密码 / 管理端用户管理
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public void updateProfile(Long userId, ProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        if (dto.getNickname() != null && !dto.getNickname().trim().isEmpty()) {
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, PasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    // ==================== 管理端 ====================

    public IPage<User> pageUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        qw.orderByDesc(User::getCreateTime);
        IPage<User> result = userMapper.selectPage(new Page<>(page, size), qw);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(400, "不能操作管理员账号");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }
}
