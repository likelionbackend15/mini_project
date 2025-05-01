package com.studybuddy.common.domain;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 도메인.
 */
public class ChatMessage {
    private Long msgId;           // PK
    private String roomId;        // 방 ID
    private String sender;        // 보낸 사람(username)
    private String content;       // 메시지 또는 이모지
    private LocalDateTime sentAt; // 전송 시각

    public ChatMessage(Long msgId, String roomId, String sender, String content, LocalDateTime sentAt) {
        this.msgId = msgId;
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
    }

    public Long getMsgId() {
        return msgId;
    }

    public void setMsgId(Long msgId) {
        this.msgId = msgId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}