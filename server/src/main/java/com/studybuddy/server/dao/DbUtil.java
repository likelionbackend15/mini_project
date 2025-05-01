package com.studybuddy.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DB 연결을 생성해 주는 유틸리티.
 */
public class DbUtil {

    /**
     * application.properties나 classpath:db.properties 에 정의된
     * JDBC URL/사용자/비밀번호를 읽어 Connection 을 반환
     *
     * Why?  
     * - 커넥션 팩토리 역할을 분리하여 재사용성과 설정 관리를 편하게 하기 위함.
     */
    public static Connection getConn() throws SQLException {
        try (InputStream in = DbUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties props = new Properties();
            props.load(in);

            String url  = props.getProperty("jdbc.url");
            String user = props.getProperty("jdbc.user");
            String pw   = props.getProperty("jdbc.password");

            return DriverManager.getConnection(url, user, pw);

        } catch (IOException e) {
            throw new SQLException("Failed to load database properties", e);
        }
    }
}
