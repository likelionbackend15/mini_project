//package com.studybuddy.common.dto;
//
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.studybuddy.common.domain.Room;
//
///**
// * 방 생성(또는 입장) 후 응답으로 돌아가는 DTO.
// * - meta: 방의 메타데이터 (domain.Room)
// * - curMembers: 현재 입장해 있는 인원 수
// */
//public class RoomInfo {
//    private final Room meta;
//    private final int  curMembers;
//
//    @JsonCreator
//    public RoomInfo(
//            @JsonProperty("meta") Room meta,
//            @JsonProperty("curMembers") int curMembers
//    ) {
//        this.meta       = meta;
//        this.curMembers = curMembers;
//    }
//
//    //----- getters -----
//    public Room getMeta()       { return meta; }
//    public int  getCurMembers() { return curMembers; }
//}


package com.studybuddy.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.studybuddy.common.domain.Room;

/**
 * 서버 ↔ 클라이언트 간 방 정보를 주고받는 DTO
 */
public class RoomInfo {
    private final String roomId;
    private final String name;
    private final int curMembers;
    private final int maxMembers;
    private final int loops;
    private final String status;
    private final boolean allowMidEntry;
    private final String hostId;

    @JsonCreator
    public RoomInfo(
            @JsonProperty("roomId") String roomId,
            @JsonProperty("name") String name,
            @JsonProperty("curMembers") int curMembers,
            @JsonProperty("maxMembers") int maxMembers,
            @JsonProperty("loops") int loops,
            @JsonProperty("status") String status,
            @JsonProperty("allowMidEntry") boolean allowMidEntry,
            @JsonProperty("hostId") String hostId
    ) {
        this.roomId = roomId;
        this.name = name;
        this.curMembers = curMembers;
        this.maxMembers = maxMembers;
        this.loops = loops;
        this.status = status;
        this.allowMidEntry = allowMidEntry;
        this.hostId = hostId;
    }

    public RoomInfo(Room r, int curMembers) {
        this(
                r.getRoomId(),
                (r.getPassword() != null && !r.getPassword().isEmpty()) ? "🔒 " + r.getName() : r.getName(),
                curMembers,
                r.getMaxMembers(),
                r.getLoops(),
                r.getStatus().name(),
                r.isAllowMidEntry(),
                r.getHostId()
        );
    }

    public String getRoomId()       { return roomId; }
    public String getName()         { return name; }
    public int getCurMembers()      { return curMembers; }
    public int getMaxMembers()      { return maxMembers; }
    public int getLoops()           { return loops; }
    public String getStatus()       { return status; }
    public boolean isAllowMidEntry(){ return allowMidEntry; }
    public String getHostId()       { return hostId; }
}
