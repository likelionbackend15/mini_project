package com.studybuddy.server;

import com.studybuddy.common.domain.Room;
import com.studybuddy.common.domain.RoomStatus;
import com.studybuddy.common.domain.User;
import com.studybuddy.common.dto.CreateRoomReq;
import com.studybuddy.server.dao.LogDAO;
import com.studybuddy.server.dao.RoomDAO;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 방 생성·조회·수정·잠금·입장 권한 로직, 통계·CSV 제공 담당
 */
public class RoomManager {

    private final RoomDAO roomDao;
    private final LogDAO  logDao;
    private final Map<String, RoomSession> sessions = new ConcurrentHashMap<>();

    public RoomManager(RoomDAO roomDao, LogDAO logDao) {
        this.roomDao = roomDao;
        this.logDao  = logDao;
    }

    /** 로비에 보여줄 OPEN 상태 방 목록(메타데이터만) 조회 */
    public List<Room> listOpenRooms() throws Exception {
        return roomDao.findOpenRooms();
    }

    /**
     * CreateRoomReq + 로그인한 User 로 새 방 만들기
     * 1) DTO → domain.Room 매핑
     * 2) DB에 INSERT
     * 3) 메모리에 RoomSession 생성·저장
     */
    public RoomSession createRoom(CreateRoomReq req, User host) throws Exception {
        // 1) UUID 생성
        String roomId = UUID.randomUUID().toString();

        // 2) DTO → 도메인 객체
        Room room = new Room(
                roomId,
                req.getName(),
                req.getMaxMembers(),
                req.isAllowMidEntry(),
                req.getFocusMin(),
                req.getBreakMin(),
                req.getLoops(),
                host.getId(),
                req.getPassword(),    // nullable 허용
                RoomStatus.OPEN       // 새로 만든 방은 항상 OPEN
        );

        // 3) DB에 저장
        roomDao.save(room);

        // 4) 세션 생성해서 메모리에 보관
        RoomSession session = new RoomSession(room, logDao);
        sessions.put(roomId, session);
        return session;
    }

    /** 방 설정 수정: DB & 세션 동기화 */
    public void modifyRoom(Room updated) throws Exception {
        roomDao.updateSettings(updated);
        RoomSession session = sessions.get(updated.getRoomId());
        if (session != null) {
            session.updateSettings(updated);
        }
    }

    /** 방 잠금: DB & 세션 변경 */
    public void lockRoom(String roomId) throws Exception {
        roomDao.updateStatus(roomId, RoomStatus.OPEN_LOCKED);
        RoomSession session = sessions.get(roomId);
        if (session != null) session.lock();
    }

    /** 공용 입장 권한 검사 후 세션에 사용자 추가 */
    public RoomSession joinRoom(String roomId, ClientHandler handler) throws Exception {
        RoomSession session = sessions.get(roomId);
        if (session == null) throw new IllegalArgumentException("No such room");
        if (!session.getMeta().isAllowMidEntry()) throw new IllegalStateException("Mid-entry not allowed");
        session.addMember(handler);
        return session;
    }

    /** 비공개 방 입장: 비밀번호 검사 → 세션 참여 */
    public RoomSession joinPrivate(String roomId, String pw, ClientHandler handler) throws Exception {
        RoomSession session = sessions.get(roomId);
        if (session == null) throw new IllegalArgumentException("No such room");
        if (!pw.equals(session.getMeta().getPassword())) throw new SecurityException("Wrong password");
        session.addMember(handler);
        return session;
    }

    /** 특정 방의 통계 집계 → RoomStats DTO 생성 (구현 생략) */
    // public RoomStats computeStats(String roomId) { ... }

    /** CSV 파일 생성 → byte[] 반환 (구현 생략) */
    // public byte[] generateCsv(String roomId, Range range) { ... }
}
