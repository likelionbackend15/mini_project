package com.studybuddy.server.util;

import com.studybuddy.common.Packet;
import com.studybuddy.server.ClientHandler;
import com.studybuddy.server.RoomSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 방의 모든 클라이언트에게 비동기 메시지 전송
 */
public final class MessageBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
    // 메시지 전송만 담당하는 스레드풀
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "msg-broadcaster-");
        t.setDaemon(true);
        return t;
    });

    // 인스턴스 만들지 못하도록 막음
    private MessageBroadcaster() {}

    /**
     * 방에 있는 모든 클라이언트에게 Packet을 보냄
     * 1) 새 스레드에서 동작해서 메인 흐름을 막지 않음
     * 2) 실패한 전송은 로그로 남기고 계속 진행
     *
     * @param session 메시지를 받을 방 세션
     * @param pkt     보낼 패킷
     * @return 전송 작업이 끝날 때 알려주는 Future
     */
    public static CompletableFuture<Void> pushAsync(RoomSession session, Packet pkt) {
        return CompletableFuture.runAsync(() -> {
            for (ClientHandler handler : session.getMembers()) {
                try {
                    handler.sendPacket(pkt);
                } catch (Exception e) {
                    log.error("메시지 전송 실패 to {}", handler, e);
                }
            }
        }, executor);
    }
}
