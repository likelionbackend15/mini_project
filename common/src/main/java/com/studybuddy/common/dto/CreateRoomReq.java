package com.studybuddy.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 방 생성 요청 페이로드.
 * Jackson 이 payloadJson 을 이 타입으로 바로 매핑하도록 사용됩니다.
 */
public class CreateRoomReq {
    private final String name;
    private final int focusMin;
    private final int breakMin;
    private final int loops;
    private final int maxMembers;
    private final boolean allowMidEntry;
    private final String password; // nullable

    @JsonCreator
    public CreateRoomReq(
            @JsonProperty("name") String name,
            @JsonProperty("focusMin") int focusMin,
            @JsonProperty("breakMin") int breakMin,
            @JsonProperty("loops") int loops,
            @JsonProperty("maxMembers") int maxMembers,
            @JsonProperty("allowMidEntry") boolean allowMidEntry,
            @JsonProperty("password") String password
    ) {
        this.name          = name;
        this.focusMin      = focusMin;
        this.breakMin      = breakMin;
        this.loops         = loops;
        this.maxMembers    = maxMembers;
        this.allowMidEntry = allowMidEntry;
        this.password      = password;
    }

    //----- getters -----
    public String  getName()          { return name; }
    public int     getFocusMin()      { return focusMin; }
    public int     getBreakMin()      { return breakMin; }
    public int     getLoops()         { return loops; }
    public int     getMaxMembers()    { return maxMembers; }
    public boolean isAllowMidEntry()  { return allowMidEntry; }
    public String  getPassword()      { return password; }
}
