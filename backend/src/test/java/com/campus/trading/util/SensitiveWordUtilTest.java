package com.campus.trading.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 敏感词 DFA 过滤器单元测试
 */
class SensitiveWordUtilTest {

    private SensitiveWordUtil util;

    @BeforeEach
    void setUp() {
        util = new SensitiveWordUtil();
        util.reload(Arrays.asList("代考", "枪支", "刷单", "办证"));
    }

    @Test
    void contains_命中敏感词() {
        assertTrue(util.contains("提供代考服务"));
        assertTrue(util.contains("代办证件"));
        assertTrue(util.contains("刷单兼职"));
    }

    @Test
    void contains_无敏感词() {
        assertFalse(util.contains("九成新教材出售"));
        assertFalse(util.contains("你好呀"));
        assertFalse(util.contains(""));
    }

    @Test
    void contains_敏感词位于边界() {
        assertTrue(util.contains("枪支"));
        assertTrue(util.contains("代考"));
    }

    @Test
    void find_返回全部命中词() {
        List<String> hits = util.find("代考和枪支都要办证");
        assertTrue(hits.contains("代考"));
        assertTrue(hits.contains("枪支"));
        assertTrue(hits.contains("办证"));
    }

    @Test
    void find_部分匹配不误报() {
        // "考证" 不应命中 "代考"
        assertFalse(util.contains("考研考证资料"));
    }

    @Test
    void reload_更新词典() {
        util.reload(Arrays.asList("违规新词"));
        assertTrue(util.contains("这是违规新词测试"));
        assertFalse(util.contains("代考"));
    }
}
