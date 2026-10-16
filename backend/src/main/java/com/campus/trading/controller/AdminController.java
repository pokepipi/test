package com.campus.trading.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trading.common.Result;
import com.campus.trading.dto.AuditDTO;
import com.campus.trading.entity.Category;
import com.campus.trading.entity.Item;
import com.campus.trading.entity.Order;
import com.campus.trading.entity.SensitiveWord;
import com.campus.trading.entity.User;
import com.campus.trading.service.CategoryService;
import com.campus.trading.service.ItemService;
import com.campus.trading.service.OrderService;
import com.campus.trading.service.SensitiveWordService;
import com.campus.trading.service.UserService;
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

import java.util.List;
import java.util.Map;

/**
 * 管理端接口 (整体受 JwtInterceptor 的 /api/admin/** 角色校验保护)
 * 覆盖: 物品审核 / 用户管理 / 物品管理 / 订单管理 / 分类管理 / 敏感词管理
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ItemService itemService;
    private final UserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final SensitiveWordService sensitiveWordService;

    // ==================== 物品审核 ====================

    @GetMapping("/audit/page")
    public Result<IPage<Item>> auditPage(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) Integer status) {
        return Result.ok(itemService.adminPage(page, size, null, status));
    }

    @PostMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, @RequestBody @Validated AuditDTO dto) {
        itemService.audit(id, dto);
        return Result.ok();
    }

    // ==================== 用户管理 ====================

    @GetMapping("/user/page")
    public Result<IPage<User>> userPage(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(userService.pageUsers(page, size, keyword));
    }

    @PutMapping("/user/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    // ==================== 物品管理 ====================

    @GetMapping("/item/page")
    public Result<IPage<Item>> itemPage(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status) {
        return Result.ok(itemService.adminPage(page, size, keyword, status));
    }

    @PutMapping("/item/{id}/status")
    public Result<Void> updateItemStatus(@PathVariable Long id, @RequestParam Integer status) {
        itemService.adminChangeStatus(id, status);
        return Result.ok();
    }

    // ==================== 订单管理 ====================

    @GetMapping("/order/page")
    public Result<IPage<Order>> orderPage(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(orderService.adminPage(page, size, status, keyword));
    }

    // ==================== 分类管理 ====================

    @PostMapping("/category")
    public Result<Void> addCategory(@RequestBody Category category) {
        categoryService.add(category.getName(), category.getParentId(), category.getSort());
        return Result.ok();
    }

    @PutMapping("/category/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        categoryService.update(id, category.getName(), category.getSort());
        return Result.ok();
    }

    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }

    // ==================== 敏感词管理 ====================

    @GetMapping("/sensitive-word/list")
    public Result<List<SensitiveWord>> sensitiveWordList() {
        return Result.ok(sensitiveWordService.listAll());
    }

    @PostMapping("/sensitive-word")
    public Result<Void> addSensitiveWord(@RequestBody Map<String, String> body) {
        sensitiveWordService.add(body.get("word"));
        return Result.ok();
    }

    @DeleteMapping("/sensitive-word/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return Result.ok();
    }

    @GetMapping("/sensitive-word/check")
    public Result<List<String>> checkSensitiveWord(@RequestParam String text) {
        return Result.ok(sensitiveWordService.check(text));
    }
}
