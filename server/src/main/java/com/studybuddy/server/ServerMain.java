package com.studybuddy.server;

import com.studybuddy.server.dao.LogDAO;
import com.studybuddy.server.dao.RoomDAO;
import com.studybuddy.server.dao.UserDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int DEFAULT_PORT = 12345;

    public static void main(String[] args) throws IOException {
        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : DEFAULT_PORT;

        // 1) DAO 인스턴스 생성
        RoomDAO roomDao = new RoomDAO();
        LogDAO  logDao  = new LogDAO();
        UserDAO userDao = new UserDAO();

        // 2) RoomManager 에 DAO 주입
        RoomManager roomMgr = new RoomManager(roomDao, logDao);

        // 3) 스레드풀 준비
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket client = ss.accept();
                System.out.println("Accepted: " + client);

                // 4) ClientHandler 에도 roomMgr, userDao 주입
                pool.submit(new ClientHandler(client, userDao, roomMgr));
            }
        }
    }
}
