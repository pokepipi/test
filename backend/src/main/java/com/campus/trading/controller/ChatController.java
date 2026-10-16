package com.campus.trading.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trading.common.Result;
import com.campus.trading.config.JwtInterceptor;
import com.campus.trading.dto.ChatSendDTO;
import com.campus.trading.entity.ChatMessage;
import com.campus.trading.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 聊天接口: 私信议价 / 会话 / 未读
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> conversations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(chatService.conversations(userId));
    }

    @GetMapping("/messages")
    public Result<IPage<ChatMessage>> messages(@RequestParam Long otherId,
                                               @RequestParam(required = false) Long itemId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "50") int size,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(chatService.messages(userId, otherId, itemId, page, size));
    }

    @PostMapping("/send")
    public Result<ChatMessage> send(@RequestBody @Validated ChatSendDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(chatService.send(userId, dto));
    }

    @GetMapping("/unread")
    public Result<Integer> unread(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(chatService.unread(userId));
    }
}
