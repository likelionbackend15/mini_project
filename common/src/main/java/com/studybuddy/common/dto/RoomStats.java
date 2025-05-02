package com.studybuddy.common.dto;

/**
 * 한 스터디룸의 누적 통계 정보를 담는 DTO(Data Transfer Object).
 *
 * ─ 서버 ──────────────────────────────────────────────
 *   • RoomManager.computeStats(...) 가 통계를 묶어
 *     RoomStats 객체로 만들어 클라이언트에게 보냄
 *
 * ─ 클라이언트 ───────────────────────────────────────
 *   • Jackson(ObjectMapper) 으로 RoomStats 를 그대로 파싱해
 *     통계 화면에 뿌린다.
 */
public class RoomStats {

    /* ---------- 필드 ---------- */
    private String roomId;        // 방 ID (UUID)
    private int    memberCount;   // 참여 인원
    private int    loopsCompleted;// 완료된 루프 수
    private int    totalFocusSec; // 누적 집중 시간(초)
    private int    totalBreakSec; // 누적 휴식 시간(초)

    /* ---------- 기본 생성자 ----------
       - Jackson 이 JSON → 객체로 만들 때 필요
     */
    public RoomStats() { }

    /* ---------- 전체 필드를 받는 생성자 ---------- */
    public RoomStats(String roomId,
                     int memberCount,
                     int loopsCompleted,
                     int totalFocusSec,
                     int totalBreakSec) {
        this.roomId         = roomId;
        this.memberCount    = memberCount;
        this.loopsCompleted = loopsCompleted;
        this.totalFocusSec  = totalFocusSec;
        this.totalBreakSec  = totalBreakSec;
    }

    /* ---------- Getter / Setter ----------
       (초급 학습용으로 직접 작성. Lombok @Data 써도 무방)
     */
    public String getRoomId()               { return roomId; }
    public void   setRoomId(String roomId)  { this.roomId = roomId; }

    public int getMemberCount()                 { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getLoopsCompleted()                 { return loopsCompleted; }
    public void setLoopsCompleted(int loopsCompleted) { this.loopsCompleted = loopsCompleted; }

    public int getTotalFocusSec()                 { return totalFocusSec; }
    public void setTotalFocusSec(int totalFocusSec) { this.totalFocusSec = totalFocusSec; }

    public int getTotalBreakSec()                 { return totalBreakSec; }
    public void setTotalBreakSec(int totalBreakSec) { this.totalBreakSec = totalBreakSec; }

    /* ---------- 편의 메서드 ----------
       → 화면에 “시:분:초” 문자열을 보여주고 싶을 때 사용할 수 있다.
     */
    public String formatSeconds(int sec) {
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    @Override public String toString() {
        return "RoomStats{" +
                "roomId='" + roomId + '\'' +
                ", memberCount=" + memberCount +
                ", loopsCompleted=" + loopsCompleted +
                ", totalFocusSec=" + totalFocusSec +
                ", totalBreakSec=" + totalBreakSec +
                '}';
    }
}
