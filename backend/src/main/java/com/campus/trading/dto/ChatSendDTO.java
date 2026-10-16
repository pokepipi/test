package com.campus.trading.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ChatSendDTO {

    @NotNull(message = "接收人不能为空")
    private Long toUserId;

    /** 关联物品(议价上下文), 可为空 */
    private Long itemId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息最长1000个字符")
    private String content;
}
