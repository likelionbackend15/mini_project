package com.studybuddy.client.model;

/**
 * 스터디룸 실시간 타이머 상태를 UI에 바인딩하기 위한 모델.
 */
public class TimerModel {
    /** 남은 시간(초) */
    private int remainingSec;

    /** 현재 단계: "FOCUS" or "BREAK" */
    private String phase;

    public TimerModel() {
        this.remainingSec = 0;
        this.phase        = "IDLE";
    }

    /** 남은 시간을 MM:SS 로 포맷팅 */
    public String getFormattedTime() {
        int minutes = remainingSec / 60;
        int seconds = remainingSec % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    //----- getters / setters -----
    public int getRemainingSec() {
        return remainingSec;
    }

    /** 서버에서 TICK 패킷이 올 때마다 업데이트 */
    public void setRemainingSec(int remainingSec) {
        this.remainingSec = remainingSec;
    }

    public String getPhase() {
        return phase;
    }

    /** "FOCUS" / "BREAK" 단계 변경 시 업데이트 */
    public void setPhase(String phase) {
        this.phase = phase;
    }
}

