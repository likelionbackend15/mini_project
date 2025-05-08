package com.studybuddy.server.dao;

import com.studybuddy.common.domain.ChatMessage;
import com.studybuddy.common.domain.TimerLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * timer_logs · chat_messages 테이블 DAO
 */
public class LogDAO {

    /* =========================================================
       1. TimerLog 저장
    ========================================================= */
    public void save(TimerLog log) throws SQLException {
        String sql = """
            INSERT INTO timer_logs(
              room_id, user_id, loop_idx, focus_sec, break_sec, ts
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
        Connection conn = DbUtil.getConn();
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString   (1, log.getRoomId());
            ps.setString     (2, log.getUserId());
            ps.setInt      (3, log.getLoopIdx());
            ps.setInt      (4, log.getFocusSec());
            ps.setInt      (5, log.getBreakSec());
            ps.setTimestamp(6, Timestamp.valueOf(log.getTs())); // ← ts 필드 저장
            ps.executeUpdate();
            DbUtil.commit(conn);
        }catch(Exception e){
            DbUtil.rollback(conn);
            e.printStackTrace();
        }finally{
            DbUtil.close(conn);
        }
    }

    /* =========================================================
       2. TimerLog 목록 조회 (방 기준)
    ========================================================= */
    public List<TimerLog> findByRoom(String roomId) throws SQLException {
        String sql = "SELECT * FROM timer_logs WHERE room_id = ?";
        List<TimerLog> list = new ArrayList<>();

        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TimerLog(
                            rs.getLong("log_id"),
                            rs.getString("room_id"),
                            rs.getString("user_id"),
                            rs.getInt("loop_idx"),
                            rs.getInt("focus_sec"),
                            rs.getInt("break_sec"),
                            rs.getTimestamp("ts").toLocalDateTime()      // ← ts 컬럼 사용
                    ));
                }
            }
        }
        return list;
    }

    /* =========================================================
       3. ChatMessage 저장 / 조회
    ========================================================= */
    public void saveChat(ChatMessage msg) throws SQLException {
        String sql = """
            INSERT INTO chat_messages(room_id, sender, content)
            VALUES (?, ?, ?)
            """;
        Connection conn = DbUtil.getConn();
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, msg.getRoomId());
            ps.setString(2, msg.getSender());
            ps.setString(3, msg.getContent());
            ps.executeUpdate();
            DbUtil.commit(conn);
        }catch(Exception e){
            DbUtil.rollback(conn);
            e.printStackTrace();
        }finally{
            DbUtil.close(conn);
        }
    }

    public List<ChatMessage> findMessagesByRoom(String roomId) throws SQLException {
        String sql = """
        SELECT 
            id AS msg_id, 
            room_id, 
            user_id, 
            message AS content, 
            created_at AS sent_at
        FROM chat_messages
        WHERE room_id = ?
        ORDER BY created_at
        """;

        List<ChatMessage> list = new ArrayList<>();

        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ChatMessage(
                            rs.getLong("msg_id"),
                            rs.getString("room_id"),
                            rs.getString("user_id"),
                            rs.getString("content"),
                            rs.getTimestamp("sent_at").toLocalDateTime()
                    ));
                }
            }
        }

        return list;
    }


    /* =========================================================
       4. 집계 쿼리 (누적 시간, 루프 수, 참여 인원)
    ========================================================= */
    public int sumFocusTime(String roomId) throws SQLException {
        return singleInt("SELECT COALESCE(SUM(focus_sec),0) FROM timer_logs WHERE room_id = ?", roomId);
    }

    public int sumBreakTime(String roomId) throws SQLException {
        return singleInt("SELECT COALESCE(SUM(break_sec),0) FROM timer_logs WHERE room_id = ?", roomId);
    }

    public int countLoops(String roomId) throws SQLException {
        return singleInt("SELECT COALESCE(MAX(loop_idx),0) FROM timer_logs WHERE room_id = ?", roomId);
    }

    public int countDistinctUsers(String roomId) throws SQLException {
        return singleInt("SELECT COUNT(DISTINCT user_id) FROM timer_logs WHERE room_id = ?", roomId);
    }

    /* =========================================================
       5. 기간별 로그 (CSV 용)
    ========================================================= */
    public List<TimerLog> findLogs(String roomId,
                                   LocalDateTime from, LocalDateTime to) throws SQLException {

        StringBuilder sb = new StringBuilder("""
            SELECT * FROM timer_logs
            WHERE room_id = ?
            """);
        if (from != null) sb.append(" AND ts >= ? ");   // ← ts 기준
        if (to   != null) sb.append(" AND ts <= ? ");

        List<TimerLog> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {

            int idx = 1;
            ps.setString(idx++, roomId);
            if (from != null) ps.setTimestamp(idx++, Timestamp.valueOf(from));
            if (to   != null) ps.setTimestamp(idx  , Timestamp.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TimerLog(
                            rs.getLong("log_id"),
                            rs.getString("room_id"),
                            rs.getString("user_id"),
                            rs.getInt("loop_idx"),
                            rs.getInt("focus_sec"),
                            rs.getInt("break_sec"),
                            rs.getTimestamp("ts").toLocalDateTime()     // ← ts
                    ));
                }
            }
        }
        return list;
    }

    /* =========================================================
       6. 내부 유틸 : 정수 하나 쉽게 얻기
    ========================================================= */
    private int singleInt(String sql, String roomId) throws SQLException {
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
