package com.campus.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存服务 (统一入口 + 降级保护)
 *
 * 设计要点:
 *  1) 所有 Redis 操作都包了 try/catch: Redis 宕机 / 未部署时自动降级为「直接查库」,
 *     业务不报错、服务照常启动 —— 缓存是优化手段, 不该成为单点故障。
 *  2) 熔断: 一旦探测到连接失败, 10 秒内不再尝试 Redis, 避免每个请求都白等一次连接超时。
 *  3) 对象缓存用 JSON 序列化; 计数器用 StringRedisTemplate, 因为 JSON 序列化会写坏 INCR。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    /** 空值占位符: 缓存穿透保护, 数据库查不到的 id 也缓存一个空标记 */
    public static final String NULL_VALUE = "__NULL__";

    /** 探测到故障后的熔断时长(毫秒) */
    private static final long BREAK_MILLIS = 10_000L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private final AtomicLong brokenUntil = new AtomicLong(0L);

    /** Redis 当前是否可用(熔断期内直接返回 false, 不再发起连接) */
    public boolean available() {
        return System.currentTimeMillis() >= brokenUntil.get();
    }

    private void markBroken(Exception e) {
        long now = System.currentTimeMillis();
        long previous = brokenUntil.getAndSet(now + BREAK_MILLIS);
        if (previous < now) {
            log.warn("[cache] Redis 不可用, 已降级为直接访问数据库(10 秒后重试): {}", e.getMessage());
        }
    }

    // ==================== 对象缓存 ====================

    /** 取缓存, 未命中或 Redis 不可用时返回 null */
    public Object get(String key) {
        if (!available()) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            markBroken(e);
            return null;
        }
    }

    public void set(String key, Object value, long ttlSeconds) {
        if (!available()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            markBroken(e);
        }
    }

    /** 写入空值标记, 用于防止恶意 id 反复穿透到数据库 */
    public void setNull(String key, long ttlSeconds) {
        set(key, NULL_VALUE, ttlSeconds);
    }

    public void delete(String key) {
        if (!available()) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            markBroken(e);
        }
    }

    // ==================== 计数器 (浏览量) ====================

    /** 自增并返回自增后的值; Redis 不可用时返回 0, 由调用方决定降级策略 */
    public long increment(String key) {
        if (!available()) {
            return 0L;
        }
        try {
            Long value = stringRedisTemplate.opsForValue().increment(key);
            return value == null ? 0L : value;
        } catch (Exception e) {
            markBroken(e);
            return 0L;
        }
    }

    /** 取出计数器当前值并删除(用于定时回写), Redis 不可用返回 0 */
    public long getAndDeleteLong(String key) {
        if (!available()) {
            return 0L;
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            stringRedisTemplate.delete(key);
            if (value == null) {
                return 0L;
            }
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            markBroken(e);
            return 0L;
        }
    }

    // ==================== 集合 (待回写的 id 清单) ====================

    public void addToSet(String key, String member) {
        if (!available()) {
            return;
        }
        try {
            stringRedisTemplate.opsForSet().add(key, member);
        } catch (Exception e) {
            markBroken(e);
        }
    }

    public void removeFromSet(String key, String member) {
        if (!available()) {
            return;
        }
        try {
            stringRedisTemplate.opsForSet().remove(key, member);
        } catch (Exception e) {
            markBroken(e);
        }
    }

    public Set<String> members(String key) {
        if (!available()) {
            return Collections.emptySet();
        }
        try {
            Set<String> members = stringRedisTemplate.opsForSet().members(key);
            return members == null ? Collections.emptySet() : members;
        } catch (Exception e) {
            markBroken(e);
            return Collections.emptySet();
        }
    }
}
