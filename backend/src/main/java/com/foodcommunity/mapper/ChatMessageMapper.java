package com.foodcommunity.mapper;

import com.foodcommunity.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息 Mapper。
 */
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    /** 当前用户参与的"最近一条消息"记录（peer 侧 + 消息侧） */
    List<ChatMessage> selectLastOfPeers(@Param("userId") Long userId);

    /** 与某人的历史（倒序分页：先取最新一页，前端倒序展示） */
    List<ChatMessage> selectHistory(@Param("userId") Long userId, @Param("peerId") Long peerId);

    /** 按对方分组的未读数：返回 [{peerId, cnt}] */
    List<Map<String, Object>> selectUnreadByPeer(@Param("userId") Long userId);

    int countUnread(@Param("userId") Long userId);

    /** 标记某方向全部已读 */
    int markRead(@Param("userId") Long userId, @Param("peerId") Long peerId);
}
