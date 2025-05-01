package com.studybuddy.client.model;

/**
 * 로비나 방 리스트 화면에서 사용할 방 정보 모델.
 */
public class RoomInfo {
    private final String roomId;
    private final String name;
    private final int    curMembers;
    private final int    maxMembers;
    private final int    loops;
    private final String status;
    private final boolean allowMidEntry;

    public RoomInfo(String roomId,
                    String name,
                    int curMembers,
                    int maxMembers,
                    int loops,
                    String status,
                    boolean allowMidEntry) {
        this.roomId        = roomId;
        this.name          = name;
        this.curMembers    = curMembers;
        this.maxMembers    = maxMembers;
        this.loops         = loops;
        this.status        = status;
        this.allowMidEntry = allowMidEntry;
    }

    //----- getters -----
    public String  getRoomId()        { return roomId; }
    public String  getName()          { return name; }
    public int     getCurMembers()    { return curMembers; }
    public int     getMaxMembers()    { return maxMembers; }
    public int     getLoops()         { return loops; }
    public String  getStatus()        { return status; }
    public boolean isAllowMidEntry()  { return allowMidEntry; }
}
