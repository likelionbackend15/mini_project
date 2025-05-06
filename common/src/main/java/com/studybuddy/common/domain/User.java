package com.studybuddy.common.domain;

import java.time.LocalDateTime;

/**
 * 사용자 정보 도메인.
 */
public class User {
    private String id;                  // PK
    private String username;          // 로그인 ID
    private String hashedPw;          // BCrypt 해시 비밀번호
    private LocalDateTime createdAt;
    private String email;// 가입 시각

    // 기본 생성자
    public User() {}

    // 전체 필드 생성자
    public User(String id, String username, String hashedPw, LocalDateTime createdAt, String email) {

        this.id = id;
        this.username = username;
        this.hashedPw = hashedPw;
        this.createdAt = createdAt;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashedPw() {
        return hashedPw;
    }

    public void setHashedPw(String hashedPw) {
        this.hashedPw = hashedPw;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
