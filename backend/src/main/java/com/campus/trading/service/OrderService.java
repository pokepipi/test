package com.campus.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trading.common.BusinessException;
import com.campus.trading.common.CacheKeys;
import com.campus.trading.dto.OrderCreateDTO;
import com.campus.trading.entity.Item;
import com.campus.trading.entity.Order;
import com.campus.trading.entity.User;
import com.campus.trading.mapper.ItemMapper;
import com.campus.trading.mapper.OrderMapper;
import com.campus.trading.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单服务: 下单 / 支付 / 发货 / 收货 / 取消, 覆盖全流程状态机
 *
 * 状态流转: 待付款(0) -> 待发货(1) -> 待收货(2) -> 已完成(3)
 *                     \-> 已取消(4)      \-> 已取消(4)
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final CacheService cacheService;

    public Order create(Long buyerId, OrderCreateDTO dto) {
        Item item = itemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new BusinessException(400, "物品不存在");
        }
        if (item.getUserId().equals(buyerId)) {
            throw new BusinessException(400, "不能购买自己发布的物品");
        }
        if (item.getStatus() != Item.STATUS_ON_SALE) {
            throw new BusinessException(400, "物品当前不可购买");
        }
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setItemId(item.getId());
        order.setBuyerId(buyerId);
        order.setSellerId(item.getUserId());
        order.setPrice(dto.getPrice());
        order.setStatus(Order.STATUS_UNPAID);
        order.setContactName(dto.getContactName());
        order.setContactPhone(dto.getContactPhone());
        order.setAddress(dto.getAddress());
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);
        return order;
    }

    /** 我的订单 (buyer/seller 视角 + 状态筛选) */
    public IPage<Order> page(Long userId, String role, int page, int size, Integer status) {
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        if ("buyer".equals(role)) {
            qw.eq(Order::getBuyerId, userId);
        } else {
            qw.eq(Order::getSellerId, userId);
        }
        if (status != null) {
            qw.eq(Order::getStatus, status);
        }
        qw.orderByDesc(Order::getCreateTime);
        IPage<Order> result = orderMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        return result;
    }

    public Order detail(Long userId, Long id) {
        Order order = requireOrder(id);
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(403, "无权查看该订单");
        }
        fillExtra(Collections.singletonList(order));
        return order;
    }

    /** 买家模拟支付: 待付款 -> 待发货, 同时锁定物品(置为已售出) */
    public void pay(Long userId, Long id) {
        Order order = requireOrder(id);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        if (order.getStatus() != Order.STATUS_UNPAID) {
            throw new BusinessException(400, "当前状态不可支付");
        }
        order.setStatus(Order.STATUS_UNSHIPPED);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        lockItem(order.getItemId());
    }

    /** 取消: 买家未付款可取消; 卖家发货前可取消并恢复物品在售 */
    public void cancel(Long userId, Long id) {
        Order order = requireOrder(id);
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(403, "无权操作该订单");
        }
        if (isBuyer && order.getStatus() == Order.STATUS_UNPAID) {
            // 买家未付款取消
        } else if (isSeller && order.getStatus() == Order.STATUS_UNSHIPPED) {
            // 卖家发货前取消, 恢复物品
            restoreItem(order.getItemId());
        } else {
            throw new BusinessException(400, "当前状态不可取消");
        }
        order.setStatus(Order.STATUS_CANCELED);
        orderMapper.updateById(order);
    }

    /** 卖家发货: 待发货 -> 待收货 */
    public void ship(Long userId, Long id) {
        Order order = requireOrder(id);
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        if (order.getStatus() != Order.STATUS_UNSHIPPED) {
            throw new BusinessException(400, "当前状态不可发货");
        }
        order.setStatus(Order.STATUS_UNRECEIVED);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 买家确认收货: 待收货 -> 已完成 */
    public void finish(Long userId, Long id) {
        Order order = requireOrder(id);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        if (order.getStatus() != Order.STATUS_UNRECEIVED) {
            throw new BusinessException(400, "当前状态不可确认收货");
        }
        order.setStatus(Order.STATUS_FINISHED);
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    // ==================== 管理端 ====================

    public IPage<Order> adminPage(int page, int size, Integer status, String keyword) {
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(Order::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Order::getOrderNo, keyword).or().like(Order::getContactName, keyword));
        }
        qw.orderByDesc(Order::getCreateTime);
        IPage<Order> result = orderMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        return result;
    }

    // ==================== 私有方法 ====================

    private Order requireOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        return order;
    }

    private void lockItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item != null && item.getStatus() == Item.STATUS_ON_SALE) {
            item.setStatus(Item.STATUS_SOLD);
            itemMapper.updateById(item);
            // 物品状态变了, 详情缓存必须失效, 否则缓存里仍显示"在售"
            cacheService.delete(CacheKeys.itemDetail(itemId));
        }
    }

    private void restoreItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item != null && item.getStatus() == Item.STATUS_SOLD) {
            item.setStatus(Item.STATUS_ON_SALE);
            itemMapper.updateById(item);
            cacheService.delete(CacheKeys.itemDetail(itemId));
        }
    }

    private String generateOrderNo() {
        return "CT" + System.currentTimeMillis() + (int) (Math.random() * 900 + 100);
    }

    private void fillExtra(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Set<Long> itemIds = orders.stream().map(Order::getItemId).collect(Collectors.toSet());
        Set<Long> userIds = orders.stream()
                .flatMap(o -> java.util.stream.Stream.of(o.getBuyerId(), o.getSellerId()))
                .collect(Collectors.toSet());
        Map<Long, Item> items = itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, i -> i));
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (Order order : orders) {
            Item item = items.get(order.getItemId());
            if (item != null) {
                order.setItemTitle(item.getTitle());
                String firstImage = item.getImages() == null ? null
                        : item.getImages().split(",")[0];
                order.setItemImage(firstImage);
            }
            User buyer = users.get(order.getBuyerId());
            if (buyer != null) {
                order.setBuyerName(buyer.getNickname());
            }
            User seller = users.get(order.getSellerId());
            if (seller != null) {
                order.setSellerName(seller.getNickname());
            }
        }
    }
}
