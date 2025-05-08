// 파일: common/src/main/java/com/studybuddy/common/dto/RoomInitResponse.java
package com.studybuddy.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.studybuddy.common.domain.ChatMessage;
import java.util.List;

public class RoomInitResponse {
    private final String roomId;
    private final int curMembers;
    private final int maxMembers;
    private final int loops;
    private final String status;
    private final boolean allowMidEntry;
    private final String hostId;
    private final List<ChatMessage> chatHistory;

    @JsonCreator
    public RoomInitResponse(
            @JsonProperty("roomId") String roomId,
            @JsonProperty("curMembers") int curMembers,
            @JsonProperty("maxMembers") int maxMembers,
            @JsonProperty("loops") int loops,
            @JsonProperty("status") String status,
            @JsonProperty("allowMidEntry") boolean allowMidEntry,
            @JsonProperty("hostId") String hostId,
            @JsonProperty("chatHistory") List<ChatMessage> chatHistory
    ) {
        this.roomId       = roomId;
        this.curMembers   = curMembers;
        this.maxMembers   = maxMembers;
        this.loops        = loops;
        this.status       = status;
        this.allowMidEntry= allowMidEntry;
        this.hostId       = hostId;
        this.chatHistory  = chatHistory;
    }

    //----- getters -----
    public String getRoomId()            { return roomId; }
    public int    getCurMembers()        { return curMembers; }
    public int    getMaxMembers()        { return maxMembers; }
    public int    getLoops()             { return loops; }
    public String getStatus()            { return status; }
    public boolean isAllowMidEntry()     { return allowMidEntry; }
    public String getHostId()            { return hostId; }
    public List<ChatMessage> getChatHistory() { return chatHistory; }
}
