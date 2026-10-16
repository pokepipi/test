package com.campus.trading.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.trading.common.CacheKeys;
import com.campus.trading.entity.Item;
import com.campus.trading.mapper.ItemMapper;
import com.campus.trading.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 浏览量回写任务
 *
 * 优化前: 每次打开物品详情都要 update item set view_count = view_count + 1,
 *         热点物品会在同一行上产生大量行锁竞争, 读请求被写放大拖慢。
 * 优化后: 浏览量先在 Redis 里 INCR(单线程原子自增, 无锁竞争),
 *         本任务每 30 秒把增量批量累加回数据库, 把 N 次写聚合成 1 次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemViewCountSyncTask {

    private final CacheService cacheService;
    private final ItemMapper itemMapper;

    @Scheduled(fixedDelayString = "${campus.cache.view-flush-interval:30000}")
    public void flushViewCount() {
        Set<String> dirtyIds = cacheService.members(CacheKeys.ITEM_VIEW_DIRTY);
        if (dirtyIds.isEmpty()) {
            return;
        }

        int flushed = 0;
        for (String idStr : dirtyIds) {
            Long itemId;
            try {
                itemId = Long.valueOf(idStr);
            } catch (NumberFormatException e) {
                cacheService.removeFromSet(CacheKeys.ITEM_VIEW_DIRTY, idStr);
                continue;
            }

            // 先摘标记、再取值: 这期间新产生的浏览量会重新打上标记留到下一轮, 不会被丢掉
            cacheService.removeFromSet(CacheKeys.ITEM_VIEW_DIRTY, idStr);
            long delta = cacheService.getAndDeleteLong(CacheKeys.itemView(itemId));
            if (delta <= 0) {
                continue;
            }

            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .setSql("view_count = view_count + " + delta));
            // 详情缓存里的浏览量已经过期, 删掉让下次读取重新载入最新值
            cacheService.delete(CacheKeys.itemDetail(itemId));
            flushed++;
        }

        if (flushed > 0) {
            log.info("[cache] 浏览量回写完成, 本轮 {} 件物品", flushed);
        }
    }
}
