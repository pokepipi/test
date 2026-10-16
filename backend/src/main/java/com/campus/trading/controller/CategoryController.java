package com.campus.trading.controller;

import com.campus.trading.common.Result;
import com.campus.trading.entity.Category;
import com.campus.trading.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类公开接口 (管理端 CRUD 见 AdminController)
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<Category>> tree() {
        return Result.ok(categoryService.tree());
    }
}
