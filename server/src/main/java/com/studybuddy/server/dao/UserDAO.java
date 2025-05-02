package com.studybuddy.server.dao;

import com.studybuddy.common.domain.User;

import java.sql.*;
import java.util.Optional;

/**
 * users 테이블에 대한 CRUD 기능 제공.
 */
public class UserDAO {

    /**
     * username 으로 사용자 조회
     *
     * What?
     *   SELECT 로 users 테이블에서 일치하는 레코드를 읽어 User 객체로 반환
     * Why?
     *   로그인·인증 시 DB 상에 해당 아이디가 있는지 확인하기 위함
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = """
            SELECT id, username, hashed_pw, created_at,email
            FROM users
            WHERE username = ?
            """;
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("hashed_pw"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getString("email")
                    );
                    return Optional.of(user);
                }
                return Optional.empty();
            }
        }
    }

    /**
     * 새 사용자 레코드 저장
     *
     * What?
     *   INSERT 하고, 생성된 PK(id)를 리턴
     * Why?
     *   회원가입 과정에서 새 사용자 정보를 DB에 영구 저장하기 위함
     */
    public long save(User user) throws SQLException {
        String sql = """
            INSERT INTO users(username, hashed_pw, email)
            VALUES (?, ?, ?)
            """;
        Connection conn = DbUtil.getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getHashedPw());
            ps.setString(3, user.getEmail());  // domain.User 에 email 필드가 있다고 가정

            ps.executeUpdate();
            DbUtil.commit(conn);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    user.setId(id);
                    return id;
                } else {
                    throw new SQLException("Failed to retrieve generated user ID");
                }
            }
        }catch(Exception e){
            DbUtil.rollback(conn);
            e.printStackTrace();
            throw new SQLException("Failed to retrieve generated user ID");
        }finally{
            DbUtil.close(conn);
        }

    }

    /**
     * username 중복 여부 확인
     *
     * What?
     *   COUNT(*) 쿼리로 이미 존재하는지 검사
     * Why?
     *   회원가입 시 같은 아이디가 이미 등록되었는지 빠르게 체크하기 위함
     */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
