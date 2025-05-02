package com.studybuddy.server;

import com.studybuddy.server.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * StudyBuddy Chat 서버 시작 파일
 *
 * 1) DB와 통신할 DAO를 만든다.
 * 2) 포트를 열고 클라이언트(사용자) 연결을 받는다.
 * 3) 들어온 클라이언트마다 ClientHandler를 새 스레드에 올려 처리한다.
 */
public class ServerMain {

    /** 로그 찍기용(화면·파일에 기록) */
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);

    /** 기본 포트번호 (명령줄 인자 없으면 12345 사용) */
    private static final int DEFAULT_PORT = 12345;

    /** OS가 동시에 받아줄 수 있는 연결 대기 수 */
    private static final int BACKLOG = 200;

    /** 동시에 돌 스레드 최대 개수 (접속자 제한용) */
    private static final int MAX_THREADS = 200;

    public static void main(String[] args) {
        /* 1. 포트 결정 */
        int port = parsePortOrDefault(args, DEFAULT_PORT);

        /* 2. DAO 만들기 (DB 접근 담당 객체) */
        RoomDAO roomDao = new RoomDAO();
        LogDAO  logDao  = new LogDAO();
        UserDAO userDao = new UserDAO();

        /* 3. 방 로직 담당 객체 */
        RoomManager roomMgr = new RoomManager(roomDao, logDao);

        /* 4. 스레드풀 준비 */
        ExecutorService pool = new ThreadPoolExecutor(
                20,               // 최소 스레드 수
                MAX_THREADS,      // 최대 스레드 수
                60L, TimeUnit.SECONDS, // 놀고 있는 추가 스레드가 60초 지나면 정리
                new LinkedBlockingQueue<>(MAX_THREADS), // 대기열 크기
                r -> {                               // 스레드 이름 붙이기
                    Thread t = new Thread(r, "client-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 꽉 차면 accept 스레드가 대신 실행
        );

        /* 5. Ctrl-C 눌렀을 때 정리 */
        //“비정상 종료”에도 서버가 깔끔하게 정리 작업을 하도록 보장
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("서버 종료 요청");
            pool.shutdownNow();
        }));

        /* 6. 소켓 열고 대기 */
        //클라이언트 연결을 실제로 받아들이는 대기실
        //BACKLOG 만큼 대기열(접속 시도 줄) 확보
        try (ServerSocket ss = new ServerSocket(port, BACKLOG)) {
            log.info("포트 {} 에서 서버 시작", port);

            while (!pool.isShutdown()) {
                Socket client = ss.accept();      // 새 연결을 기다림
                log.debug("새 접속: {}", client.getRemoteSocketAddress());

                // 클라이언트마다 별도 스레드에서 처리
                pool.submit(new ClientHandler(client, userDao, roomMgr));
            }
        } catch (IOException e) {
            log.error("소켓 accept 중 오류", e);
        }
    }

    //명령줄 인자(args)로 받은 문자열을 포트 번호(정수)로 바꿔주는 역할
    private static int parsePortOrDefault(String[] args, int def) {
        if (args.length == 0) return def;
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("잘못된 포트 '" + args[0] + "' → 기본값 " + def + " 사용");
            return def;
        }
    }
}
