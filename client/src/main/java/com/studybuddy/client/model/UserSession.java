package com.studybuddy.client.model;

import com.studybuddy.common.domain.User;

/**
 * 현재 로그인된 사용자 정보를 보관하는 싱글톤 세션.
 */
public class UserSession {
    private static UserSession instance;

    /** 로그인된 사용자 도메인 객체 */
    private User currentUser;

    private UserSession() { }

    /** 전역 세션 객체 얻기 */
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /** 로그인 처리 후 호출 */
    public void setUser(User user) {
        this.currentUser = user;
    }

    /** 로그아웃 처리 후 호출 */
    public void clear() {
        this.currentUser = null;
    }

    /** 로그인 여부 */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** 현재 사용자 도메인 객체 조회 */
    public User getCurrentUser() {
        return currentUser;
    }
}
