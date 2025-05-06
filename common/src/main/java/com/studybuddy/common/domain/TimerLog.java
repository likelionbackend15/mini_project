package com.studybuddy.common.domain;

import java.time.LocalDateTime;

/**
 * 타이머 루프별 기록 도메인.
 */
public class TimerLog {
    private Long logId;           // PK
    private String roomId;        // 방 ID
    private String userId;        // 사용자 ID (String 타입으로 변경)
    private int loopIdx;          // 반복 인덱스
    private int focusSec;         // 실제 집중 시간(초)
    private int breakSec;         // 실제 휴식 시간(초)
    private LocalDateTime ts;     // 기록 시작 시각

    public TimerLog(Long logId, String roomId, String userId,
                    int loopIdx, int focusSec, int breakSec,
                    LocalDateTime ts) {
        this.logId = logId;
        this.roomId = roomId;
        this.userId = userId;
        this.loopIdx = loopIdx;
        this.focusSec = focusSec;
        this.breakSec = breakSec;
        this.ts = ts;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getLoopIdx() {
        return loopIdx;
    }

    public void setLoopIdx(int loopIdx) {
        this.loopIdx = loopIdx;
    }

    public int getFocusSec() {
        return focusSec;
    }

    public void setFocusSec(int focusSec) {
        this.focusSec = focusSec;
    }

    public int getBreakSec() {
        return breakSec;
    }

    public void setBreakSec(int breakSec) {
        this.breakSec = breakSec;
    }

    public LocalDateTime getTs() {
        return ts;
    }

    public void setTs(LocalDateTime ts) {
        this.ts = ts;
    }
}
