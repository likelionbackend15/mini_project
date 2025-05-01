package com.studybuddy.common.domain;

/**
 * 스터디룸의 상태(단계)를 나타내는 열거형.
 */
public enum RoomStatus {
    OPEN,         // 대기 중 (입장 가능)
    OPEN_LOCKED,  // 잠금 상태, 단 비밀번호 맞으면 입장 가능
    RUNNING,      // 타이머 실행 중
    CLOSED        // 세션이 종료되어 입장 불가
}
