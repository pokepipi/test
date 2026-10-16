package com.campus.trading.controller;

import com.campus.trading.common.Result;
import com.campus.trading.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 交易数据统计接口 (管理端可视化, 需 ADMIN 角色)
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.trend(days));
    }

    @GetMapping("/category")
    public Result<List<Map<String, Object>>> category() {
        return Result.ok(statsService.categoryStats());
    }

    @GetMapping("/top-items")
    public Result<List<Map<String, Object>>> topItems(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.topItems(limit));
    }

    @GetMapping("/status")
    public Result<List<Map<String, Object>>> status() {
        return Result.ok(statsService.statusDist());
    }

    @GetMapping("/user-growth")
    public Result<List<Map<String, Object>>> userGrowth(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.userGrowth(days));
    }
}
