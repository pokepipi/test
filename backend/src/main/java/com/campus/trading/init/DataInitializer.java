package com.campus.trading.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trading.entity.User;
import com.campus.trading.mapper.UserMapper;
import com.campus.trading.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动初始化: 创建默认管理员/演示账号, 加载敏感词词典
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SensitiveWordService sensitiveWordService;

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")) == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("系统管理员");
            admin.setRole(User.ROLE_ADMIN);
            admin.setStatus(User.STATUS_NORMAL);
            userMapper.insert(admin);
            log.info("初始化管理员账号: admin / admin123");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "student")) == 0) {
            User student = new User();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("123456"));
            student.setNickname("学生小张");
            student.setRole(User.ROLE_STUDENT);
            student.setStatus(User.STATUS_NORMAL);
            userMapper.insert(student);
            log.info("初始化演示学生账号: student / 123456");
        }
        sensitiveWordService.reload();
        log.info("敏感词词典加载完成, 共 {} 条", sensitiveWordService.list().size());
    }
}
