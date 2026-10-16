package com.campus.trading.service;

import com.campus.trading.entity.Item;
import com.campus.trading.mapper.ItemMapper;
import com.campus.trading.mapper.OrderMapper;
import com.campus.trading.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易数据统计服务 (管理端可视化)
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final OrderMapper orderMapper;

    /** 核心指标概览 */
    public Map<String, Object> overview() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userCount", userMapper.selectCount(null));
        map.put("itemCount", itemMapper.selectCount(null));
        map.put("onSaleCount", itemMapper.selectCountByStatus(Item.STATUS_ON_SALE));
        map.put("pendingAuditCount", itemMapper.selectCountByStatus(Item.STATUS_PENDING));
        map.put("orderCount", orderMapper.selectCount(null));
        map.put("totalAmount", orderMapper.selectTotalAmount());
        map.put("finishedAmount", orderMapper.selectFinishedAmount());
        map.put("totalViews", itemMapper.selectTotalViews());
        return map;
    }

    /** 近 N 天成交趋势 */
    public List<Map<String, Object>> trend(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days - 1L).with(LocalTime.MIN);
        return orderMapper.selectTrend(start);
    }

    /** 分类成交分布 */
    public List<Map<String, Object>> categoryStats() {
        return orderMapper.selectCategoryStats();
    }

    /** 热销物品排行 */
    public List<Map<String, Object>> topItems(int limit) {
        return orderMapper.selectTopItems(limit);
    }

    /** 订单状态分布 */
    public List<Map<String, Object>> statusDist() {
        return orderMapper.selectStatusDist();
    }

    /** 近 N 天用户增长 */
    public List<Map<String, Object>> userGrowth(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days - 1L).with(LocalTime.MIN);
        return orderMapper.selectUserGrowth(start);
    }
}
