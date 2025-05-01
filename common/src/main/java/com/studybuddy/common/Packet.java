package com.studybuddy.common;

/**
 * PacketType 과 JSON 페이로드를 묶어 전송하는 DTO.
 */
public record Packet(
        PacketType type,
        String payloadJson
) {}
