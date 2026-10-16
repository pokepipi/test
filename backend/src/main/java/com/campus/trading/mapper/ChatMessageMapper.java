package com.campus.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trading.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 我的会话列表: 按 (物品, 双方) 分组取最后一条消息
     */
    @Select("SELECT * FROM chat_message WHERE id IN (" +
            "  SELECT MAX(id) FROM chat_message" +
            "  WHERE from_user_id = #{uid} OR to_user_id = #{uid}" +
            "  GROUP BY item_id, LEAST(from_user_id, to_user_id), GREATEST(from_user_id, to_user_id)" +
            ") ORDER BY id DESC")
    List<ChatMessage> selectConversations(@Param("uid") Long uid);

    @Select("SELECT COUNT(*) FROM chat_message WHERE to_user_id = #{uid} AND is_read = 0")
    int countUnread(@Param("uid") Long uid);

    @Select("SELECT COUNT(*) FROM chat_message WHERE to_user_id = #{uid} AND is_read = 0" +
            " AND from_user_id = #{otherId} AND (item_id <=> #{itemId})")
    int countUnreadWith(@Param("uid") Long uid, @Param("otherId") Long otherId, @Param("itemId") Long itemId);
}
