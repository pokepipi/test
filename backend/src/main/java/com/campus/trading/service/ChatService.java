package com.campus.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trading.common.BusinessException;
import com.campus.trading.config.ChatWebSocketHandler;
import com.campus.trading.dto.ChatSendDTO;
import com.campus.trading.entity.ChatMessage;
import com.campus.trading.entity.Item;
import com.campus.trading.entity.User;
import com.campus.trading.mapper.ChatMessageMapper;
import com.campus.trading.mapper.ItemMapper;
import com.campus.trading.mapper.UserMapper;
import com.campus.trading.util.SensitiveWordUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天服务: 私信议价 / 会话列表 / 未读 / WebSocket 推送
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final SensitiveWordUtil sensitiveWordUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 发送消息 (敏感词拦截 + WebSocket 实时推送) */
    public ChatMessage send(Long fromUserId, ChatSendDTO dto) {
        if (fromUserId.equals(dto.getToUserId())) {
            throw new BusinessException(400, "不能给自己发消息");
        }
        if (userMapper.selectById(dto.getToUserId()) == null) {
            throw new BusinessException(400, "接收人不存在");
        }
        if (dto.getItemId() != null && itemMapper.selectById(dto.getItemId()) == null) {
            throw new BusinessException(400, "物品不存在");
        }
        if (sensitiveWordUtil.contains(dto.getContent())) {
            throw new BusinessException(400, "消息包含违规内容, 已拦截");
        }
        ChatMessage msg = new ChatMessage();
        msg.setFromUserId(fromUserId);
        msg.setToUserId(dto.getToUserId());
        msg.setItemId(dto.getItemId());
        msg.setContent(dto.getContent());
        msg.setIsRead(0);
        chatMessageMapper.insert(msg);

        // WebSocket 实时推送
        User from = userMapper.selectById(fromUserId);
        Map<String, Object> push = new LinkedHashMap<>();
        push.put("type", "new_message");
        push.put("id", msg.getId());
        push.put("fromUserId", fromUserId);
        push.put("fromNickname", from == null ? "" : from.getNickname());
        push.put("fromAvatar", from == null ? null : from.getAvatar());
        push.put("itemId", msg.getItemId());
        push.put("content", msg.getContent());
        push.put("createTime", msg.getCreateTime());
        try {
            ChatWebSocketHandler.pushTo(dto.getToUserId(), objectMapper.writeValueAsString(push));
        } catch (Exception ignore) {
        }
        return msg;
    }

    /** 会话列表: 每个 (对方+物品) 取最后一条 */
    public List<Map<String, Object>> conversations(Long userId) {
        List<ChatMessage> list = chatMessageMapper.selectConversations(userId);
        Map<Long, User> userCache = new HashMap<>();
        Map<Long, String> itemCache = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : list) {
            Long otherId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            User other = userCache.computeIfAbsent(otherId, userMapper::selectById);
            String itemTitle = null;
            if (msg.getItemId() != null) {
                itemTitle = itemCache.computeIfAbsent(msg.getItemId(), id -> {
                    Item item = itemMapper.selectById(id);
                    return item == null ? null : item.getTitle();
                });
            }
            int unread = chatMessageMapper.countUnreadWith(userId, otherId, msg.getItemId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("otherId", otherId);
            map.put("otherName", other == null ? "用户" : other.getNickname());
            map.put("otherAvatar", other == null ? null : other.getAvatar());
            map.put("itemId", msg.getItemId());
            map.put("itemTitle", itemTitle);
            map.put("lastMessage", msg.getContent());
            map.put("lastTime", msg.getCreateTime());
            map.put("unread", unread);
            result.add(map);
        }
        return result;
    }

    /** 与某人的聊天记录 (拉取后自动标记已读) */
    public IPage<ChatMessage> messages(Long userId, Long otherId, Long itemId, int page, int size) {
        LambdaQueryWrapper<ChatMessage> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.and(x -> x.eq(ChatMessage::getFromUserId, userId).eq(ChatMessage::getToUserId, otherId))
                .or(x -> x.eq(ChatMessage::getFromUserId, otherId).eq(ChatMessage::getToUserId, userId)));
        if (itemId != null) {
            qw.eq(ChatMessage::getItemId, itemId);
        } else {
            qw.isNull(ChatMessage::getItemId);
        }
        qw.orderByAsc(ChatMessage::getId);
        IPage<ChatMessage> result = chatMessageMapper.selectPage(new Page<>(page, size), qw);

        // 标记已读
        LambdaUpdateWrapper<ChatMessage> read = new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getFromUserId, otherId)
                .eq(ChatMessage::getIsRead, 0);
        if (itemId != null) {
            read.eq(ChatMessage::getItemId, itemId);
        } else {
            read.isNull(ChatMessage::getItemId);
        }
        ChatMessage update = new ChatMessage();
        update.setIsRead(1);
        chatMessageMapper.update(update, read);

        fillFrom(result.getRecords());
        return result;
    }

    public int unread(Long userId) {
        return chatMessageMapper.countUnread(userId);
    }

    private void fillFrom(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        Set<Long> userIds = messages.stream().map(ChatMessage::getFromUserId).collect(Collectors.toSet());
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (ChatMessage msg : messages) {
            User user = users.get(msg.getFromUserId());
            if (user != null) {
                msg.setFromNickname(user.getNickname());
                msg.setFromAvatar(user.getAvatar());
            }
        }
    }
}
