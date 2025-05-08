package com.studybuddy.client.model;

public class RoomInfo {
    private final String roomId;
    private final String name;
    private final String hostId;
    private final int curMembers;
    private final int maxMembers;
    private final int loops;
    private final String status;
    private final boolean allowMidEntry;

    public RoomInfo(String roomId, String name, String hostId, int curMembers, int maxMembers, int loops, String status, boolean allowMidEntry) {
        this.roomId = roomId;
        this.name = name;
        this.hostId = hostId;
        this.curMembers = curMembers;
        this.maxMembers = maxMembers;
        this.loops = loops;
        this.status = status;
        this.allowMidEntry = allowMidEntry;
    }

    public String getRoomId() { return roomId; }
    public String getName() { return name; }
    public String getHostId() { return hostId; }
    public int getCurMembers() { return curMembers; }
    public int getMaxMembers() { return maxMembers; }
    public int getLoops() { return loops; }
    public String getStatus() { return status; }
    public boolean isAllowMidEntry() { return allowMidEntry; }
}

//의존성이 중복됩니다! 해당 클래스 삭제 요망