package com.studybuddy.server.dao;

import com.studybuddy.common.domain.ChatMessage;
import com.studybuddy.common.domain.TimerLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 타이머 로그(timer_logs)와 채팅 로그(chat_messages) CRUD 담당.
 */
public class LogDAO {

    /**
     * 집중/휴식 타이머 로그 저장
     *
     * What?
     *   INSERT INTO timer_logs(...)
     * Why?
     *   통계 및 CSV 원본 데이터로 활용하기 위해 각 세션 결과를 기록
     */
    public void save(TimerLog log) throws SQLException {
        String sql = """
            INSERT INTO timer_logs(
              room_id, user_id, loop_idx, focus_sec, break_sec
            ) VALUES (?, ?, ?, ?, ?)
            """;
        Connection conn = DbUtil.getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, log.getRoomId());
            ps.setLong(2, log.getUserId());
            ps.setInt(3, log.getLoopIdx());
            ps.setInt(4, log.getFocusSec());
            ps.setInt(5, log.getBreakSec());

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
     * 특정 방의 타이머 로그 목록 조회
     */
    public List<TimerLog> findByRoom(String roomId) throws SQLException {
        String sql = "SELECT * FROM timer_logs WHERE room_id = ?";
        List<TimerLog> list = new ArrayList<>();

        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TimerLog log = new TimerLog(
                            rs.getLong("log_id"),
                            rs.getString("room_id"),
                            rs.getLong("user_id"),
                            rs.getInt("loop_idx"),
                            rs.getInt("focus_sec"),
                            rs.getInt("break_sec"),
                            rs.getTimestamp("logged_at").toLocalDateTime()
                    );
                    list.add(log);
                }
            }
        }
        return list;
    }

    /**
     * 채팅 메시지 저장
     */
    public void saveChat(ChatMessage msg) throws SQLException {
        String sql = """
            INSERT INTO chat_messages(room_id, sender, content)
            VALUES (?, ?, ?)
            """;
        Connection conn = DbUtil.getConn();
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

    /**
     * 특정 방의 채팅 기록 조회
     */
    public List<ChatMessage> findMessagesByRoom(String roomId) throws SQLException {
        String sql = """
            SELECT msg_id, room_id, sender, content, sent_at
            FROM chat_messages
            WHERE room_id = ?
            ORDER BY sent_at
            """;
        List<ChatMessage> list = new ArrayList<>();

        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage msg = new ChatMessage(
                            rs.getLong("msg_id"),
                            rs.getString("room_id"),
                            rs.getString("sender"),
                            rs.getString("content"),
                            rs.getTimestamp("sent_at").toLocalDateTime()
                    );
                    list.add(msg);
                }
            }
        }
        return list;
    }
}
