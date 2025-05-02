package com.studybuddy.common.dto;

import java.util.List;

public class StatsResponse {
    private final String roomId;
    private final String roomName;
    private final int loopCount;
    private final List<UserStats> statsList;


    public StatsResponse(String roomId, String roomName, int loopCount, List<UserStats> statsList) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.loopCount = loopCount;
        this.statsList = statsList;
    }

    public String getRoomId()     { return roomId; }
    public String getRoomName()   { return roomName; }
    public int getLoopCount()     { return loopCount; }
    public List<UserStats> getStatsList() { return statsList; }

    public static class UserStats {
        private final String username; // 유저 이름
        private final int totalFocusSec; // 총 집중 시간
        private final int totalBreakSec; //총 쉬는시간
        private final int chatCount; // 채팅쵯수


        public UserStats(String username, int totalFocusSec, int totalBreakSec, int chatCount) {
            this.username = username;
            this.totalFocusSec = totalFocusSec;
            this.totalBreakSec = totalBreakSec;
            this.chatCount = chatCount;
        }

        public String getUsername()       { return username; }
        public int getTotalFocusSec()     { return totalFocusSec; }
        public int getTotalBreakSec()     { return totalBreakSec; }
        public int getChatCount()       { return chatCount; }
    }
}
