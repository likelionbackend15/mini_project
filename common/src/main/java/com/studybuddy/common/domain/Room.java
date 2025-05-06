package com.studybuddy.common.domain;

/**
 * 스터디룸 메타데이터 도메인.
 */
public class Room {
    private String roomId;         // UUID
    private String name;           // 방 이름
    private int maxMembers;        // 최대 인원
    private boolean allowMidEntry; // 중간 입장 허용 여부
    private int focusMin;          // 집중 시간(분)
    private int breakMin;          // 휴식 시간(분)
    private int loops;             // 반복 횟수
    private String hostId;           // 방장 User.id
    private String password;       //프라이빗 방 비밀번호 (nullable)
    private RoomStatus status;     // OPEN, LOCKED, RUNNING, CLOSED


    public Room(String roomId, String name, int maxMembers, boolean allowMidEntry, int focusMin, int breakMin, int loops, String  hostId,String password, RoomStatus status) {
        this.roomId = roomId;
        this.name = name;
        this.maxMembers = maxMembers;
        this.allowMidEntry = allowMidEntry;
        this.focusMin = focusMin;
        this.breakMin = breakMin;
        this.loops = loops;
        this.hostId = hostId;
        this.password = password;
        this.status = status;


    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public boolean isAllowMidEntry() {
        return allowMidEntry;
    }

    public void setAllowMidEntry(boolean allowMidEntry) {
        this.allowMidEntry = allowMidEntry;
    }

    public int getFocusMin() {
        return focusMin;
    }

    public void setFocusMin(int focusMin) {
        this.focusMin = focusMin;
    }

    public int getBreakMin() {
        return breakMin;
    }

    public void setBreakMin(int breakMin) {
        this.breakMin = breakMin;
    }

    public int getLoops() {
        return loops;
    }

    public void setLoops(int loops) {
        this.loops = loops;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
