package com.campus.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trading.common.BusinessException;
import com.campus.trading.common.CacheKeys;
import com.campus.trading.dto.AuditDTO;
import com.campus.trading.dto.PublishItemDTO;
import com.campus.trading.entity.Category;
import com.campus.trading.entity.Item;
import com.campus.trading.entity.User;
import com.campus.trading.mapper.CategoryMapper;
import com.campus.trading.mapper.ItemMapper;
import com.campus.trading.mapper.UserMapper;
import com.campus.trading.util.SensitiveWordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 物品服务: 浏览检索 / 发布 / 上下架 / 审核
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final CacheService cacheService;

    /** 物品详情缓存时长(秒) */
    @Value("${campus.cache.item-detail-ttl:300}")
    private int detailTtlSeconds;

    /** 空值缓存时长(秒), 用于防穿透 */
    @Value("${campus.cache.item-null-ttl:60}")
    private int nullTtlSeconds;

    /** 公开分页检索 (仅在售) */
    public IPage<Item> page(int page, int size, String keyword, Long categoryId,
                            BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        LambdaQueryWrapper<Item> qw = new LambdaQueryWrapper<>();
        qw.eq(Item::getStatus, Item.STATUS_ON_SALE);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Item::getTitle, keyword).or().like(Item::getDescription, keyword));
        }
        if (categoryId != null) {
            List<Long> childIds = categoryMapper.selectList(
                            new LambdaQueryWrapper<Category>().eq(Category::getParentId, categoryId))
                    .stream().map(Category::getId).collect(Collectors.toList());
            if (childIds.isEmpty()) {
                qw.eq(Item::getCategoryId, categoryId);
            } else {
                childIds.add(categoryId);
                qw.in(Item::getCategoryId, childIds);
            }
        }
        if (minPrice != null) {
            qw.ge(Item::getPrice, minPrice);
        }
        if (maxPrice != null) {
            qw.le(Item::getPrice, maxPrice);
        }
        if ("price_asc".equals(sort)) {
            qw.orderByAsc(Item::getPrice);
        } else if ("price_desc".equals(sort)) {
            qw.orderByDesc(Item::getPrice);
        } else if ("hot".equals(sort)) {
            qw.orderByDesc(Item::getViewCount);
        } else {
            qw.orderByDesc(Item::getCreateTime);
        }
        IPage<Item> result = itemMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        return result;
    }

    /**
     * 物品详情, 浏览量 +1 (待审核/驳回物品对外不可见)
     *
     * 缓存策略:
     *  - 详情整体缓存(含卖家昵称/分类名), 命中时 0 次数据库查询
     *  - 查不到或未上架时缓存空值, 防止伪造 id 反复穿透打库
     *  - TTL 带随机抖动, 避免大量 key 同时失效引发缓存雪崩
     *  - 浏览量走 Redis 计数 + 定时回写, Redis 挂了则降级为直接写库
     */
    public Item detail(Long id) {
        String cacheKey = CacheKeys.itemDetail(id);
        Object cached = cacheService.get(cacheKey);

        Item item;
        if (cached != null) {
            if (CacheService.NULL_VALUE.equals(cached)) {
                throw new BusinessException(400, "物品不存在或已下架");
            }
            item = (Item) cached;
        } else {
            item = itemMapper.selectById(id);
            if (item == null) {
                cacheService.setNull(cacheKey, nullTtlSeconds);
                throw new BusinessException(400, "物品不存在或已下架");
            }
            if (item.getStatus() == Item.STATUS_PENDING || item.getStatus() == Item.STATUS_REJECTED) {
                cacheService.setNull(cacheKey, nullTtlSeconds);
                throw new BusinessException(400, "物品未上架或审核未通过");
            }
            fillExtra(Collections.singletonList(item));
            cacheService.set(cacheKey, item,
                    detailTtlSeconds + ThreadLocalRandom.current().nextInt(60));
        }

        int base = item.getViewCount() == null ? 0 : item.getViewCount();
        long delta = cacheService.increment(CacheKeys.itemView(id));
        if (delta > 0) {
            cacheService.addToSet(CacheKeys.ITEM_VIEW_DIRTY, String.valueOf(id));
            item.setViewCount(base + (int) delta);
        } else {
            // Redis 不可用: 降级为原来的直接写库, 保证计数不丢
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, id).setSql("view_count = view_count + 1"));
            item.setViewCount(base + 1);
        }
        return item;
    }

    /** 发布物品: 自动敏感词扫描, 进入待审核 */
    public Long publish(Long userId, PublishItemDTO dto) {
        checkCategory(dto.getCategoryId());
        Item item = new Item();
        item.setUserId(userId);
        item.setCategoryId(dto.getCategoryId());
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImages(dto.getImages());
        item.setStatus(Item.STATUS_PENDING);
        item.setViewCount(0);
        item.setSensitiveHit(scanSensitive(dto) ? 1 : 0);
        itemMapper.insert(item);
        return item.getId();
    }

    /** 修改物品: 修改后需重新审核 */
    public void update(Long userId, Long id, PublishItemDTO dto) {
        Item item = requireOwner(userId, id);
        checkCategory(dto.getCategoryId());
        item.setCategoryId(dto.getCategoryId());
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        if (dto.getImages() != null) {
            item.setImages(dto.getImages());
        }
        item.setStatus(Item.STATUS_PENDING);
        item.setSensitiveHit(scanSensitive(dto) ? 1 : 0);
        item.setRejectReason(null);
        itemMapper.updateById(item);
        evictDetail(id);
    }

    /** 卖家上/下架 */
    public void changeStatus(Long userId, Long id, Integer status) {
        Item item = requireOwner(userId, id);
        if (status != Item.STATUS_ON_SALE && status != Item.STATUS_OFF) {
            throw new BusinessException(400, "非法操作");
        }
        item.setStatus(status);
        itemMapper.updateById(item);
        evictDetail(id);
    }

    public void delete(Long userId, Long id) {
        Item item = requireOwner(userId, id);
        itemMapper.deleteById(item.getId());
        evictDetail(id);
    }

    /** 我发布的物品 */
    public IPage<Item> myItems(Long userId, int page, int size, Integer status) {
        LambdaQueryWrapper<Item> qw = new LambdaQueryWrapper<Item>().eq(Item::getUserId, userId);
        if (status != null) {
            qw.eq(Item::getStatus, status);
        }
        qw.orderByDesc(Item::getCreateTime);
        IPage<Item> result = itemMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        return result;
    }

    // ==================== 管理端 ====================

    public IPage<Item> adminPage(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<Item> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(Item::getTitle, keyword);
        }
        if (status != null) {
            qw.eq(Item::getStatus, status);
        }
        qw.orderByAsc(Item::getStatus).orderByDesc(Item::getCreateTime);
        IPage<Item> result = itemMapper.selectPage(new Page<>(page, size), qw);
        fillExtra(result.getRecords());
        return result;
    }

    /** 审核: approve 通过 / reject 驳回 */
    public void audit(Long id, AuditDTO dto) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(400, "物品不存在");
        }
        if ("approve".equals(dto.getAction())) {
            item.setStatus(Item.STATUS_ON_SALE);
            item.setRejectReason(null);
        } else if ("reject".equals(dto.getAction())) {
            item.setStatus(Item.STATUS_REJECTED);
            item.setRejectReason(dto.getReason());
        } else {
            throw new BusinessException(400, "非法审核操作");
        }
        itemMapper.updateById(item);
        evictDetail(id);
    }

    public void adminChangeStatus(Long id, Integer status) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(400, "物品不存在");
        }
        item.setStatus(status);
        itemMapper.updateById(item);
        evictDetail(id);
    }

    // ==================== 私有方法 ====================

    /**
     * 删除物品详情缓存
     *
     * 采用「更新数据库后直接删缓存」而不是「更新缓存」:
     * 删缓存是幂等的, 且并发下最多引发一次回源, 不会像更新缓存那样出现新旧值互相覆盖。
     */
    private void evictDetail(Long id) {
        cacheService.delete(CacheKeys.itemDetail(id));
    }

    private Item requireOwner(Long userId, Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(400, "物品不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该物品");
        }
        return item;
    }

    private void checkCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(400, "分类不存在");
        }
    }

    private boolean scanSensitive(PublishItemDTO dto) {
        String text = (dto.getTitle() == null ? "" : dto.getTitle())
                + " " + (dto.getDescription() == null ? "" : dto.getDescription());
        return sensitiveWordUtil.contains(text);
    }

    /** 填充卖家昵称/头像与分类名称 */
    private void fillExtra(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Set<Long> userIds = items.stream().map(Item::getUserId).collect(Collectors.toSet());
        Set<Long> catIds = items.stream().map(Item::getCategoryId).collect(Collectors.toSet());
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Category> cats = categoryMapper.selectBatchIds(catIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
        for (Item item : items) {
            User user = users.get(item.getUserId());
            if (user != null) {
                item.setSellerName(user.getNickname());
                item.setSellerAvatar(user.getAvatar());
            }
            Category category = cats.get(item.getCategoryId());
            if (category != null) {
                item.setCategoryName(category.getName());
            }
        }
    }
}
