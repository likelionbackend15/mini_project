package com.studybuddy.common.dto;

import com.studybuddy.common.domain.Room;

/**
 * 방 생성(또는 입장) 후 응답으로 돌아가는 DTO.
 * - meta: 방의 메타데이터 (domain.Room)
 * - curMembers: 현재 입장해 있는 인원 수
 */
public class RoomInfo {
    private final Room meta;
    private final int  curMembers;

    public RoomInfo(Room meta, int curMembers) {
        this.meta       = meta;
        this.curMembers = curMembers;
    }

    //----- getters -----
    public Room getMeta()        { return meta; }
    public int  getCurMembers()  { return curMembers; }
}
