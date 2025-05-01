package com.studybuddy.client.net;

import com.studybuddy.common.Packet;

/**
 * ClientSocket 으로부터 도착한 Packet 을 처리할 콜백 인터페이스.
 */
public interface PacketListener {
    /**
     * 서버로부터 정상적으로 수신된 Packet 을 처리.
     * @param packet 수신된 Packet
     */
    void onPacket(Packet packet);

    /**
     * 수신 중 오류가 발생했을 때 호출.
     * @param ex 발생한 예외
     */
    void onError(Exception ex);
}
