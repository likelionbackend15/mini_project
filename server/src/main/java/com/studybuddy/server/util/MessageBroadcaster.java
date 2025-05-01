package com.studybuddy.server.util;

import com.studybuddy.common.Packet;
import com.studybuddy.server.ClientHandler;
import com.studybuddy.server.RoomSession;

import java.util.concurrent.CompletableFuture;

/**
 * MessageBroadcaster는 방(RoomSession)에 속한 모든 클라이언트에
 * 지연 없이 비동기적으로 Packet을 전송하기 위한 유틸 클래스입니다.
 */
public final class MessageBroadcaster {

    private MessageBroadcaster() {
        // 유틸 클래스이므로 인스턴스화 금지
    }

    /**
     * What?
     *   주어진 RoomSession에 속한 모든 ClientHandler에게 Packet을 전송합니다.
     * Why?
     *   느린 클라이언트가 있더라도 별도의 스레드에서 비동기로 전송하여
     *   전체 서비스 성능 저하를 방지하기 위함
     *
     * @param session 브로드캐스트 대상 방 세션
     * @param pkt     전송할 Packet 객체
     * @return CompletableFuture<Void> – 전송 완료 시점을 알리는 Future
     */
    public static CompletableFuture<Void> pushAsync(RoomSession session, Packet pkt) {
        // CompletableFuture.runAsync: ForkJoinPool.commonPool()을 사용해 비동기 실행
        return CompletableFuture.runAsync(() -> {
            // 방에 연결된 모든 클라이언트에게 순차적으로 전송
            for (ClientHandler handler : session.getMembers()) {
                try {
                    // ClientHandler#sendPacket 메서드는 패킷 JSON을 소켓으로 출력합니다.
                    handler.sendPacket(pkt);
                } catch (Exception e) {
                    // 전송 중 오류 발생 시 로그만 남기고 계속 진행
                    e.printStackTrace();
                }
            }
        });
    }
}
