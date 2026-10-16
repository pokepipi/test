package com.campus.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trading.common.BusinessException;
import com.campus.trading.entity.SensitiveWord;
import com.campus.trading.mapper.SensitiveWordMapper;
import com.campus.trading.util.SensitiveWordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感词服务: 词典管理 + 内容检测 (违规内容审核)
 */
@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordUtil sensitiveWordUtil;

    public List<String> list() {
        return sensitiveWordMapper.selectList(null).stream()
                .map(SensitiveWord::getWord).collect(Collectors.toList());
    }

    /** 管理端: 返回含 ID 的完整列表 */
    public List<SensitiveWord> listAll() {
        return sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<SensitiveWord>().orderByAsc(SensitiveWord::getId));
    }

    public void add(String word) {
        if (word == null || word.trim().isEmpty()) {
            throw new BusinessException(400, "敏感词不能为空");
        }
        String w = word.trim();
        Long count = sensitiveWordMapper.selectCount(
                new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getWord, w));
        if (count > 0) {
            throw new BusinessException(400, "敏感词已存在");
        }
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(w);
        sensitiveWordMapper.insert(sw);
        reload();
    }

    public void delete(Long id) {
        sensitiveWordMapper.deleteById(id);
        reload();
    }

    /** 检测文本, 返回命中的敏感词列表 */
    public List<String> check(String text) {
        return sensitiveWordUtil.find(text);
    }

    /** 重建 DFA 字典树 */
    public void reload() {
        sensitiveWordUtil.reload(list());
    }
}
