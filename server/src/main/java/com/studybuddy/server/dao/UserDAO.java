package com.studybuddy.server.dao;

import com.studybuddy.common.domain.User;

import java.sql.*;
import java.util.Optional;

/**
 * users 테이블 DAO
 *   - id : VARCHAR(50)  (사용자가 지정, PK·중복 불가)
 *   - username : VARCHAR(100) (닉네임, 중복 허용)
 */
public class UserDAO {

    /* ─────────────────────────────────────────
       1. id(로그인 ID)로 단건 조회
    ───────────────────────────────────────── */
    public Optional<User> findById(String id) throws SQLException {
        String sql = """
            SELECT id, username, hashed_pw, created_at, email
            FROM users
            WHERE id = ?
            """;
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs))
                        : Optional.empty();
            }
        }
    }

    /* (참고) 닉네임으로 조회 – 필요 시 사용 */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs))
                        : Optional.empty();
            }
        }
    }

    /* ─────────────────────────────────────────
       2. 새 사용자 저장  (id는 클라이언트가 지정)
    ───────────────────────────────────────── */
    public String save(User user) throws SQLException {
        String sql = """
            INSERT INTO users(id, username, hashed_pw, email)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getId());        // 클라이언트 입력 ID
            ps.setString(2, user.getUsername());  // 닉네임
            ps.setString(3, user.getHashedPw());
            ps.setString(4, user.getEmail());
            ps.executeUpdate();
            return user.getId();
        } catch (SQLIntegrityConstraintViolationException dup) {
            // PK(id) 또는 email UNIQUE 충돌
            throw new SQLException("ID 또는 Email 이 이미 존재합니다.", dup);
        }
    }

    /* ─────────────────────────────────────────
       3. ID 중복 체크
    ───────────────────────────────────────── */
    public boolean existsById(String id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /* ─────────────────────────────────────────
       4. email 로 조회
    ───────────────────────────────────────── */
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs))
                        : Optional.empty();
            }
        }
    }

    /* ─────────────────────────────────────────
       5. 비밀번호 해시 업데이트
    ───────────────────────────────────────── */
    public void updatePassword(String id, String newHash) throws SQLException {
        String sql = "UPDATE users SET hashed_pw = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newHash);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    /* ─────────────────────────────────────────
       6. ResultSet → User 매핑
    ───────────────────────────────────────── */
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("hashed_pw"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getString("email")
        );
    }
}
