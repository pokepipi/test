package com.campus.trading;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手物品交易平台 - 启动类
 *
 * @EnableScheduling 用于开启定时任务(浏览量从 Redis 批量回写数据库)
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.campus.trading.mapper")
public class CampusTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusTradingApplication.class, args);
    }
}
