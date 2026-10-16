package com.campus.trading.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trading.common.Result;
import com.campus.trading.config.JwtInterceptor;
import com.campus.trading.dto.OrderCreateDTO;
import com.campus.trading.entity.Order;
import com.campus.trading.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 订单接口: 下单 / 支付 / 发货 / 收货 / 取消 (全流程状态机)
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public Result<Order> create(@RequestBody @Validated OrderCreateDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(orderService.create(userId, dto));
    }

    /** role: buyer-我买到的 seller-我卖出的 */
    @GetMapping("/page")
    public Result<IPage<Order>> page(@RequestParam(defaultValue = "buyer") String role,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(orderService.page(userId, role, page, size, status));
    }

    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(orderService.detail(userId, id));
    }

    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, HttpServletRequest request) {
        orderService.pay((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        orderService.cancel((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, HttpServletRequest request) {
        orderService.ship((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id, HttpServletRequest request) {
        orderService.finish((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }
}
