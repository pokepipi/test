package com.campus.trading.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trading.common.Result;
import com.campus.trading.config.JwtInterceptor;
import com.campus.trading.dto.PublishItemDTO;
import com.campus.trading.entity.Item;
import com.campus.trading.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

/**
 * 物品接口: 浏览检索(公开) / 发布 / 管理
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /** 公开: 分页检索在售物品 */
    @GetMapping("/page")
    public Result<IPage<Item>> page(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "12") int size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) BigDecimal minPrice,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(required = false) String sort) {
        return Result.ok(itemService.page(page, size, keyword, categoryId, minPrice, maxPrice, sort));
    }

    /** 公开: 物品详情 */
    @GetMapping("/{id}")
    public Result<Item> detail(@PathVariable Long id) {
        return Result.ok(itemService.detail(id));
    }

    /** 发布物品 */
    @PostMapping
    public Result<Long> publish(@RequestBody @Validated PublishItemDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(itemService.publish(userId, dto));
    }

    /** 修改物品 (重新审核) */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated PublishItemDTO dto,
                               HttpServletRequest request) {
        itemService.update((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id, dto);
        return Result.ok();
    }

    /** 上架/下架 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status,
                                     HttpServletRequest request) {
        itemService.changeStatus((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        itemService.delete((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), id);
        return Result.ok();
    }

    /** 我发布的物品 */
    @GetMapping("/my")
    public Result<IPage<Item>> my(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) Integer status,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(itemService.myItems(userId, page, size, status));
    }
}
