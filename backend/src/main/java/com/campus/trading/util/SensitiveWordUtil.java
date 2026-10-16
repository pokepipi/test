package com.campus.trading.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词过滤工具 (DFA 有限状态自动机)
 *
 * 将敏感词词典构建为一棵字典树:
 *   词 "代考" -> {代 -> {考 -> {END}}}
 * 匹配时逐字符沿树前进, 走到 END 即命中, 时间复杂度 O(n)。
 */
@Component
@SuppressWarnings({"rawtypes", "unchecked"})
public class SensitiveWordUtil {

    /** 字典树结束标记 */
    private static final Character END = '\0';

    private volatile Map<Character, Map> wordMap = new HashMap<>();

    /**
     * 用最新词典重建字典树 (启动与词典变更时调用)
     */
    public synchronized void reload(List<String> words) {
        Map<Character, Map> root = new HashMap<>();
        if (words != null) {
            for (String word : words) {
                if (word == null || word.trim().isEmpty()) {
                    continue;
                }
                Map<Character, Map> cur = root;
                for (int i = 0; i < word.length(); i++) {
                    char c = word.charAt(i);
                    Map<Character, Map> next = cur.get(c);
                    if (next == null) {
                        next = new HashMap<>();
                        cur.put(c, next);
                    }
                    if (i == word.length() - 1) {
                        next.put(END, null);
                    }
                    cur = next;
                }
            }
        }
        this.wordMap = root;
    }

    /**
     * 查找文本中命中的敏感词列表
     */
    public List<String> find(String text) {
        List<String> hits = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return hits;
        }
        for (int i = 0; i < text.length(); i++) {
            Map<Character, Map> cur = wordMap;
            StringBuilder sb = new StringBuilder();
            int j = i;
            while (j < text.length() && cur != null) {
                Map<Character, Map> next = cur.get(text.charAt(j));
                if (next == null) {
                    break;
                }
                sb.append(text.charAt(j));
                if (next.containsKey(END)) {
                    hits.add(sb.toString());
                }
                cur = next;
                j++;
            }
        }
        return hits;
    }

    public boolean contains(String text) {
        return !find(text).isEmpty();
    }
}
