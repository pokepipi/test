package com.campus.trading.common;

/**
 * 缓存 key 统一管理 (集中定义, 避免散落在业务代码里拼字符串)
 *
 * 命名规范: 项目前缀 : 业务模块 : 数据类型 : 业务主键
 */
public final class CacheKeys {

    private static final String PREFIX = "campus:";

    /** 物品详情缓存 */
    private static final String ITEM_DETAIL = PREFIX + "item:detail:";

    /** 物品浏览量计数器 (Redis 累加, 定时回写数据库) */
    private static final String ITEM_VIEW = PREFIX + "item:view:";

    /** 有浏览量待回写的物品 id 集合 (比 KEYS 扫描更安全) */
    public static final String ITEM_VIEW_DIRTY = PREFIX + "item:view:dirty";

    private CacheKeys() {
    }

    public static String itemDetail(Long itemId) {
        return ITEM_DETAIL + itemId;
    }

    public static String itemView(Long itemId) {
        return ITEM_VIEW + itemId;
    }
}
