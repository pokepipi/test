package com.campus.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trading.common.BusinessException;
import com.campus.trading.entity.Category;
import com.campus.trading.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类服务: 树形查询 / 管理端 CRUD
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    /** 返回两级分类树 */
    public List<Category> tree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort).orderByAsc(Category::getId));
        Map<Long, Category> map = new LinkedHashMap<>();
        for (Category c : all) {
            c.setChildren(new ArrayList<>());
            map.put(c.getId(), c);
        }
        List<Category> roots = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() == null || c.getParentId() == 0L) {
                roots.add(c);
            } else {
                Category parent = map.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(c);
                } else {
                    roots.add(c);
                }
            }
        }
        return roots;
    }

    public void add(String name, Long parentId, Integer sort) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        Category category = new Category();
        category.setName(name.trim());
        category.setParentId(parentId == null ? 0L : parentId);
        category.setSort(sort == null ? 0 : sort);
        categoryMapper.insert(category);
    }

    public void update(Long id, String name, Integer sort) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(400, "分类不存在");
        }
        if (name != null && !name.trim().isEmpty()) {
            category.setName(name.trim());
        }
        if (sort != null) {
            category.setSort(sort);
        }
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(400, "该分类下存在子分类, 无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
