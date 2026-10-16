package com.campus.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trading.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT COALESCE(SUM(price), 0) FROM orders WHERE status != 4")
    BigDecimal selectTotalAmount();

    @Select("SELECT COALESCE(SUM(price), 0) FROM orders WHERE status = 3")
    BigDecimal selectFinishedAmount();

    /** 近 N 天成交趋势 (排除已取消) */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS count, COALESCE(SUM(price), 0) AS amount" +
            " FROM orders WHERE status != 4 AND create_time >= #{start}" +
            " GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> selectTrend(@Param("start") LocalDateTime start);

    /** 各分类成交分布 */
    @Select("SELECT c.name AS name, COUNT(*) AS count, COALESCE(SUM(o.price), 0) AS amount" +
            " FROM orders o JOIN item i ON o.item_id = i.id JOIN category c ON i.category_id = c.id" +
            " WHERE o.status != 4 GROUP BY i.category_id ORDER BY amount DESC")
    List<Map<String, Object>> selectCategoryStats();

    /** 热销物品排行 */
    @Select("SELECT o.item_id AS itemId, i.title AS title, i.images AS images, COUNT(*) AS count, COALESCE(SUM(o.price), 0) AS amount" +
            " FROM orders o JOIN item i ON o.item_id = i.id" +
            " WHERE o.status != 4 GROUP BY o.item_id ORDER BY amount DESC LIMIT #{limit}")
    List<Map<String, Object>> selectTopItems(@Param("limit") int limit);

    /** 订单状态分布 */
    @Select("SELECT status, COUNT(*) AS count FROM orders GROUP BY status")
    List<Map<String, Object>> selectStatusDist();

    /** 近 N 天用户增长 */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS count FROM user" +
            " WHERE create_time >= #{start} GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> selectUserGrowth(@Param("start") LocalDateTime start);
}
