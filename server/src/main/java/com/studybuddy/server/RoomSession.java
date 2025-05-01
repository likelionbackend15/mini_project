package com.studybuddy.server;

import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.domain.Room;
import com.studybuddy.common.domain.RoomStatus;
import com.studybuddy.common.domain.TimerLog;
import com.studybuddy.server.dao.LogDAO;
import com.studybuddy.server.util.MessageBroadcaster;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한 방(스터디룸)의 runtime 세션.
 *   - 방 설정(meta), 접속 중인 클라이언트,
 *     타이머, 루프별 임시 TimerLog 보관 관리
 */
public class RoomSession {

    /** 방 설정 메타 */
    private Room meta;

    /** 실시간 접속 중인 클라이언트 핸들러 모음 */
    private final Set<ClientHandler> members = new CopyOnWriteArraySet<>();

    /** 방 전용 타이머 사이클 관리 */
    private final PomodoroTimer timer;

    /** 루프별 사용자별 임시 타이머 기록 */
    private final Map<Long, List<TimerLog>> localLogs = new ConcurrentHashMap<>();

    public RoomSession(Room meta, LogDAO logDao) {
        this.meta = meta;
        this.timer = new PomodoroTimer(this, logDao);
    }

    public Room getMeta() {
        return meta;
    }

    /** 모든 클라이언트에 비동기 브로드캐스트 */
    public void broadcast(Packet pkt) {
        MessageBroadcaster.pushAsync(this, pkt);
    }

    /**
     * RoomSession 에 참여된 클라이언트 목록을 반환
     * MessageBroadcaster 에서 비동기 브로드캐스트할 때 사용
     */
    public Set<ClientHandler> getMembers() {
        // 외부에서 이 Set을 건드리지 않도록 읽기 전용 래핑
        return Collections.unmodifiableSet(members);
    }

    /** 한 명 입장 처리 */
    public void addMember(ClientHandler ch) {
        members.add(ch);
        // 입장 알림 broadcast 가능
    }

    /** 한 명 퇴장 처리 */
    public void removeMember(ClientHandler ch) {
        members.remove(ch);
        // 퇴장 알림 broadcast 가능
    }

    /** 방 설정 변경 시 meta 갱신 */
    public void updateSettings(Room updated) {
        this.meta = updated;
    }

    /** 설정 변경 잠금: UI 비활성화 등 */
    public void lock() {
        meta.setStatus(RoomStatus.OPEN_LOCKED);
    }

    /** 집중 단계 시작 요청 */
    public void startFocus() {
        broadcast(new Packet(PacketType.TIMER_FOCUS_START, ""));
        timer.startFocus(meta.getFocusMin() * 60);
    }

    /** 휴식 단계 시작 요청 */
    public void startBreak() {
        broadcast(new Packet(PacketType.TIMER_BREAK_START, ""));
        timer.startBreak(meta.getBreakMin() * 60);
    }
}
