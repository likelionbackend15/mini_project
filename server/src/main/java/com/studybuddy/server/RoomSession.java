package com.studybuddy.server;

import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.domain.Room;
import com.studybuddy.common.domain.RoomStatus;
import com.studybuddy.server.PomodoroTimer;
import com.studybuddy.server.dao.LogDAO;
import com.studybuddy.server.util.MessageBroadcaster;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 한 스터디룸의 런타임 세션을 관리
 *  • 방 정보(meta)
 *  • 접속 중인 클라이언트 모음
 *  • 타이머 제어(start/stop)
 */
public class RoomSession {

    private Room meta;                             // 방 설정 정보
    private final Set<ClientHandler> members =     // 접속 중인 클라이언트 핸들러
            new CopyOnWriteArraySet<>();
    private final PomodoroTimer timer;             // 방 전용 뽀모도로 타이머

    public RoomSession(Room meta, LogDAO logDao) {
        this.meta = meta;
        this.timer = new PomodoroTimer(this, logDao);
    }

    /** 현재 방 설정 조회 */
    public Room getMeta() {
        return meta;
    }

    /** 클라이언트 입장 처리 */
    public void addMember(ClientHandler ch) {
        members.add(ch);

    /* ───────── 입장 알림 ─────────
       PacketType.USER_JOINED 가 enum에 없다면
       시스템 채팅(CHAT)으로 대체해도 됩니다. */
        if (ch.getUser() != null) {
            String nick = ch.getUser().getUsername();
            broadcast(new Packet(
                    PacketType.CHAT,
                    "{\"sender\":\"SYSTEM\",\"text\":\"" + nick + " 님이 입장했습니다.\"}"
            ));
        }
    }

    /** 클라이언트 퇴장 처리 */
    public void removeMember(ClientHandler ch) {
        members.remove(ch);

        // 시스템 메시지로 퇴장 알림 전송
        String nick = ch.getUser() != null ? ch.getUser().getUsername() : "알 수 없는 사용자";
        String text = nick + "님이 퇴장했습니다.";

        broadcast(new Packet(
                PacketType.CHAT,
                "{\"sender\":\"SYSTEM\",\"text\":\"" + text + "\"}"
        ));
    }

    /** 접속 중인 클라이언트 목록 반환 (읽기 전용) */
    public Set<ClientHandler> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    /** 설정이 변경되면 호출 */
    public void updateSettings(Room updated) {
        this.meta = updated;
    }

    /** 방 잠금 상태로 변경 */
    public void lock() {
        meta.setStatus(RoomStatus.OPEN_LOCKED);
    }

    /** 집중 단계 시작: 알림 후 타이머 실행 */
    public void startFocus() {
        MessageBroadcaster.pushAsync(this,
                new Packet(PacketType.TIMER_FOCUS_START, ""));
        timer.startFocus(meta.getFocusMin() * 60);
    }

    /** 휴식 단계 시작: 알림 후 타이머 실행 */
    public void startBreak() {
        MessageBroadcaster.pushAsync(this,
                new Packet(PacketType.TIMER_BREAK_START, ""));
        timer.startBreak(meta.getBreakMin() * 60);
    }

    /** 내부: 타이머가 보낼 패킷을 멤버들에게 전송 */
    public void broadcast(Packet pkt) {
        MessageBroadcaster.pushAsync(this, pkt);
    }
}
