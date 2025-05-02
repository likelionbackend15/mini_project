package com.studybuddy.server;

import com.studybuddy.common.domain.Room;
import com.studybuddy.common.domain.RoomStatus;
import com.studybuddy.common.domain.TimerLog;
import com.studybuddy.common.domain.User;
import com.studybuddy.common.dto.CreateRoomReq;
import com.studybuddy.common.dto.RoomStats;
import com.studybuddy.server.dao.LogDAO;
import com.studybuddy.server.dao.RoomDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 방(스터디룸) 관련 모든 서버-사이드 작업 담당
 *  └ 목록 조회, 생성·수정·잠금, 입장 권한, 통계·CSV
 */
public class RoomManager {

    private static final Logger log = LoggerFactory.getLogger(RoomManager.class);

    private final RoomDAO roomDao;   // 방 메타 DB
    private final LogDAO  logDao;    // 타이머·채팅 로그 DB

    /** 실행 중인 방 세션들 (roomId → RoomSession) */
    private final Map<String, RoomSession> sessions = new ConcurrentHashMap<>();

    /* ---------- 기본 기능 ---------- */

    public RoomManager(RoomDAO roomDao, LogDAO logDao) {
        this.roomDao = roomDao;
        this.logDao  = logDao;
    }

    /** 로비에 보여줄 OPEN 방 목록 */
    public List<Room> listOpenRooms() throws Exception {
        return roomDao.findOpenRooms();
    }

    /** 방 생성 */
    public RoomSession createRoom(CreateRoomReq req, User host) throws Exception {
        String roomId = UUID.randomUUID().toString();

        Room room = new Room(roomId, req.getName(), req.getMaxMembers(),
                req.isAllowMidEntry(), req.getFocusMin(), req.getBreakMin(),
                req.getLoops(), host.getId(), req.getPassword(), RoomStatus.OPEN);

        roomDao.save(room);
        RoomSession session = new RoomSession(room, logDao);
        sessions.put(roomId, session);
        return session;
    }

    /** 방 설정 변경 */
    public void modifyRoom(Room updated) throws Exception {
        roomDao.updateSettings(updated);
        RoomSession s = sessions.get(updated.getRoomId());
        if (s != null) s.updateSettings(updated);
    }

    /** 방 잠금 */
    public void lockRoom(String roomId) throws Exception {
        roomDao.updateStatus(roomId, RoomStatus.OPEN_LOCKED);
        RoomSession s = sessions.get(roomId);
        if (s != null) s.lock();
    }

    /* ---------- 입장 권한 ---------- */

    public RoomSession joinRoom(String roomId, ClientHandler h) throws Exception {
        RoomSession s = sessions.get(roomId);
        if (s == null)           throw new IllegalArgumentException("room not found");
        if (!s.getMeta().isAllowMidEntry()) throw new IllegalStateException("mid-entry blocked");
        s.addMember(h);
        return s;
    }

    public RoomSession joinPrivate(String roomId, String pw, ClientHandler h) throws Exception {
        RoomSession s = sessions.get(roomId);
        if (s == null)                 throw new IllegalArgumentException("room not found");
        if (!pw.equals(s.getMeta().getPassword())) throw new SecurityException("bad password");
        s.addMember(h);
        return s;
    }

    /* ---------- 통계 ---------- */

    public RoomStats computeStats(String roomId) throws Exception {
        RoomSession s = sessions.get(roomId);

        int focus  = logDao.sumFocusTime(roomId);
        int brk    = logDao.sumBreakTime(roomId);
        int loops  = logDao.countLoops(roomId);
        int member = (s != null) ? s.getMembers().size()
                : logDao.countDistinctUsers(roomId);

        return new RoomStats(roomId, member, loops, focus, brk);
    }

    /* ---------- CSV ---------- */

    public byte[] generateCsv(String roomId,
                              LocalDateTime from, LocalDateTime to) throws Exception {

        List<TimerLog> logs = logDao.findLogs(roomId, from, to);

        StringJoiner csv = new StringJoiner("\n");
        csv.add("userId,loop,focusSec,breakSec,ts");          // 헤더
        for (TimerLog l : logs) {
            csv.add(String.format("%d,%d,%d,%d,%s",
                    l.getUserId(), l.getLoopIdx(),
                    l.getFocusSec(), l.getBreakSec(),
                    l.getTs()));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
