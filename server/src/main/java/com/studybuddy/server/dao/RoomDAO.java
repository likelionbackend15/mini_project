package com.studybuddy.server.dao;


import com.studybuddy.common.domain.Room;
import com.studybuddy.common.domain.RoomStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * rooms 테이블에 대한 CRUD 기능 제공.
 */
public class RoomDAO {

    /**
     * 새 방 메타데이터 저장
     *
     * What?
     *   INSERT INTO rooms (room_id, name, host_id, max_members,
     *                      allow_mid_entry, focus_min, break_min,
     *                      loops, password, status)
     * Why?
     *   방 생성 요청을 DB에 영구 저장하여 나중에 조회·통계 등에 활용하기 위함
     */
    public void save(Room room) throws SQLException {
        String sql = """
            INSERT INTO rooms(
              room_id, name, host_id, max_members,
              allow_mid_entry, focus_min, break_min,
              loops, password, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        Connection conn = DbUtil.getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, room.getRoomId());
            ps.setString(2, room.getName());
            ps.setLong(3, room.getHostId());
            ps.setInt(4, room.getMaxMembers());
            ps.setBoolean(5, room.isAllowMidEntry());
            ps.setInt(6, room.getFocusMin());
            ps.setInt(7, room.getBreakMin());
            ps.setInt(8, room.getLoops());
            ps.setString(9, room.getPassword());          // password 추가
            ps.setString(10, room.getStatus().name());    // status 추가

            ps.executeUpdate();
            DbUtil.commit(conn);
        }catch(Exception e){
            DbUtil.rollback(conn);
            e.printStackTrace();
        }finally{
            DbUtil.close(conn);
        }
    }

    /**
     * OPEN 상태인 방 목록 조회
     *
     * What?
     *   SELECT * FROM rooms WHERE status = 'OPEN'
     * Why?
     *   로비에서 참여 가능한 방 리스트를 보여주기 위함
     */
    public List<Room> findOpenRooms() throws SQLException {
        String sql = "SELECT * FROM rooms WHERE status = 'OPEN'";
        List<Room> list = new ArrayList<>();

        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToRoom(rs));
            }
        }
        return list;
    }

    /**
     * 방 설정값(인원, 시간, 비밀번호 등) 업데이트
     *
     * What?
     *   UPDATE rooms SET ... WHERE room_id = ?
     * Why?
     *   방장의 설정 변경 요청을 DB에 반영하고,
     *   클라이언트에 최신 상태를 방송하기 위함
     */
    public void updateSettings(Room room) throws SQLException {
        String sql = """
            UPDATE rooms
            SET max_members      = ?,
                focus_min        = ?,
                break_min        = ?,
                loops            = ?,
                allow_mid_entry  = ?,
                password         = ?
            WHERE room_id = ?
            """;
        Connection conn = DbUtil.getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, room.getMaxMembers());
            ps.setInt(2, room.getFocusMin());
            ps.setInt(3, room.getBreakMin());
            ps.setInt(4, room.getLoops());
            ps.setBoolean(5, room.isAllowMidEntry());
            ps.setString(6, room.getPassword());  // password 반영
            ps.setString(7, room.getRoomId());

            ps.executeUpdate();
            DbUtil.commit(conn);
        }catch(Exception e){
            DbUtil.rollback(conn);
            e.printStackTrace();
        }finally{
            DbUtil.close(conn);
        }

    }

    /**
     * 방 상태(LOCKED, RUNNING, CLOSED) 변경
     */
    public void updateStatus(String roomId, RoomStatus status) throws SQLException {
        String sql = "UPDATE rooms SET status = ? WHERE room_id = ?";
        Connection conn = DbUtil.getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, roomId);

            ps.executeUpdate();
            DbUtil.commit(conn);
        }catch(Exception e){
            DbUtil.rollback(conn);
            e.printStackTrace();
        }finally{
            DbUtil.close(conn);
        }

    }

    /**
     * room_id 로 단일 방 조회
     */
    public Optional<Room> findById(String roomId) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE room_id = ?";
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRoom(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * ResultSet → Room 객체 매핑 헬퍼
     *
     * What?
     *   ResultSet 컬럼값을 읽어 Room 인스턴스에 담는다
     * Why?
     *   중복 코드를 줄이고, 일관성 있게 매핑하기 위함
     */
    private Room mapRowToRoom(ResultSet rs) throws SQLException {
        return new Room(
                rs.getString("room_id"),
                rs.getString("name"),
                rs.getInt("max_members"),
                rs.getBoolean("allow_mid_entry"),
                rs.getInt("focus_min"),
                rs.getInt("break_min"),
                rs.getInt("loops"),
                rs.getLong("host_id"),
                rs.getString("password"),
                RoomStatus.valueOf(rs.getString("status"))
        );
    }
}
