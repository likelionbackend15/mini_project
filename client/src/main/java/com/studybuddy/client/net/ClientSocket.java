package com.studybuddy.client.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.common.Packet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 서버와의 TCP 연결을 관리하고,
 * JSON Packet 송수신을 담당하는 유틸리티 클래스.
 */
public class ClientSocket {
    private Socket socket; // 서버랑 통신할 소켓
    private BufferedReader in; // 서버에서 들어오는 데이터 읽기
    private PrintWriter out; // 서버로 데이터 보내기
    private final ObjectMapper mapper = new ObjectMapper();

    /** 수신된 Packet 을 처리할 리스너 목록 */
    private final List<PacketListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 서버에 연결하고, 백그라운드에서 메시지 수신 루프를 돌린다.
     *
     * @param host 서버 호스트
     * @param port 서버 포트
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port); // 서버에 연결
        in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out    = new PrintWriter(socket.getOutputStream(), true);

        // 수신 루프를 별도 스레드로 실행
        Thread recvThread = new Thread(this::receiveLoop, "ClientSocket-Receiver");
        recvThread.setDaemon(true);
        recvThread.start();
    }

    /** 연결 종료 */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            notifyError(e);
        }
    }

    /**
     * 서버로 Packet(JSON) 전송
     *
     */
    public void sendPacket(Packet pkt) throws IOException {
        try {
            String json = mapper.writeValueAsString(pkt);
            out.println(json);
        } catch (IOException e) {
            notifyError(e); // 리스너에 에러 전달
        }
    }

    /**
     * 서버로부터 들어오는 JSON 문자열을 Packet 으로 역직렬화하고
     * 등록된 모든 리스너에 전달한다.
     */
    private void receiveLoop() {
        String line;
        try {
            while ((line = in.readLine()) != null) {
                try {
                    Packet pkt = mapper.readValue(line, Packet.class);
                    for (PacketListener l : listeners) {
                        l.onPacket(pkt);
                    }
                } catch (Exception ex) {
                    notifyError(ex);
                }
            }
        } catch (IOException e) {
            disconnect();
            notifyError(e);
        }
    }

    public PrintWriter getWriter() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }


    /**
     * PacketListener 등록.
     * @param listener 새로 추가할 리스너
     */
    public void addListener(PacketListener listener) {
        listeners.add(listener);
    }

    /** PacketListener 제거 */
    public void removeListener(PacketListener listener) {
        listeners.remove(listener);
    }


    /* 내부 예외 발생 시 listener에게 에러 전달*/
    private void notifyError(Exception e) {
        for (PacketListener l : listeners) {
            l.onError(e);
        }
    }

}
