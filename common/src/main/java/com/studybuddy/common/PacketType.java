package com.studybuddy.common;

/**
 * 서버↔클라이언트 간 주고받는 패킷 종류를 정의
 */
public enum PacketType {
    // 회원 관리
    LOGIN,       // 로그인 요청/응답
    SIGNUP,      // 회원가입 요청/응답
    SEND_CODE,         // 이메일 인증 코드 전송
    RESET_PASSWORD,    // 비밀번호 재설정 요청
    SETTING_USER,    //사용자 정보

    // 로비
    LIST_ROOMS,      // 방 목록 조회
    CREATE_ROOM,     // 방 생성
    JOIN_ROOM,       // 공개 방 입장
    JOIN_PRIVATE,    // 비공개 방 입장
    BACK_TO_LOBBY,   // 로비로 복귀

    // 방 설정
    MODIFY_ROOM,     // 방 설정 변경
    LOCK_ROOM,       // 방 잠금

    // 타이머 흐름
    TIMER_FOCUS_START,  // 집중 단계 시작
    TIMER_BREAK_START,  // 휴식 단계 시작
    TIMER_TICK,         // 남은 시간 브로드캐스트
    TIMER_END,          // 세션 종료

    // 채팅
    CHAT,           // 채팅 메시지

    // 통계
    ROOM_STATS,     // 통계 요청
    STATS_VIEW,     // 통계 조회
    DOWNLOAD_CSV,   // CSV 다운로드

    // 공통 응답
    ACK,            // 성공 응답
    ERROR           // 오류 응답
}

