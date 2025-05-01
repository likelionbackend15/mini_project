package com.studybuddy.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.domain.User;
import com.studybuddy.common.dto.CreateRoomReq;
import com.studybuddy.common.dto.RoomInfo;
import com.studybuddy.server.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;

/**
 * 클라이언트별 네트워크 터널.
 *   - 로그인/방 생성/입장/채팅/타이머 제어 등의 요청을 처리하여
 *     JSON 직렬화 Packet으로 응답 전송.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final UserDAO userDao;
    private final RoomManager roomMgr;

    private BufferedReader in;
    private PrintWriter    out;
    private User user;      // 로그인 완료 후 정보
    private RoomSession    curRoom;   // 현재 입장 세션

    private static final ObjectMapper mapper = new ObjectMapper();

    public ClientHandler(Socket socket, UserDAO userDao, RoomManager roomMgr) {
        this.socket  = socket;
        this.userDao = userDao;
        this.roomMgr = roomMgr;
    }

    @Override
    public void run() {
        try {
            // 스트림 열기
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 무한 루프: 클라이언트로부터 Packet(JSON) 수신
            String line;
            while ((line = in.readLine()) != null) {
                Packet pkt = mapper.readValue(line, Packet.class);
                dispatch(pkt);
            }
        } catch (IOException e) {
            // 클라이언트 연결 종료 or 예외
            System.err.println("Client disconnected: " + socket.getRemoteSocketAddress());
        }
    }

    /** 패킷 종류별로 핸들러 메서드 호출 */
    private void dispatch(Packet pkt) throws IOException {
        switch (pkt.type()) {
            case LOGIN            -> handleLogin(pkt);
            case CREATE_ROOM      -> handleCreateRoom(pkt);
            case JOIN_ROOM        -> handleJoinRoom(pkt);
            case JOIN_PRIVATE     -> handleJoinPrivate(pkt);
            case TIMER_FOCUS_START,
                 TIMER_BREAK_START-> handleTimerStart(pkt);
            case CHAT             -> handleChat(pkt);
            case BACK_TO_LOBBY    -> handleBackToLobby(pkt);
            // TODO: STATS_VIEW, DOWNLOAD_CSV 등 추가
            default               -> sendError("Unknown packet type: " + pkt.type());
        }
    }

    /** 로그인 요청 처리 */
    private void handleLogin(Packet pkt) throws IOException {
        // payloadJson 예시: {"username":"alice","password":"pwd"}
        var req = mapper.readTree(pkt.payloadJson());
        String username = req.get("username").asText();
        String password = req.get("password").asText();

        try {
            Optional<User> opt = userDao.findByUsername(username);
            if (opt.isPresent() && BCrypt.checkpw(password, opt.get().getHashedPw())) {
                this.user = opt.get();
                sendAck(PacketType.ACK, mapper.writeValueAsString(user));
            } else {
                sendError("Invalid credentials");
            }
        } catch (Exception e) {
            sendError("Login failed: " + e.getMessage());
        }
    }

    /** 공용 방 입장 */
    private void handleJoinRoom(Packet pkt) throws IOException {
        var req = mapper.readTree(pkt.payloadJson());
        String roomId = req.get("roomId").asText();

        try {
            // 1) 세션 가져오기
            RoomSession session = roomMgr.joinRoom(roomId, this);
            // 2) 세션에 이 핸들러(클라이언트) 등록
            session.addMember(this);
            // 3) 현재 세션 보관
            this.curRoom = session;
            // 4) ACK
            sendAck(PacketType.ACK, "");
        } catch (Exception e) {
            sendError("Join room failed: " + e.getMessage());
        }
    }


    private void handleCreateRoom(Packet pkt) throws IOException {
        // 1) JSON → CreateRoomReq
        var reqNode = mapper.readTree(pkt.payloadJson());
        CreateRoomReq cr = mapper.treeToValue(reqNode, CreateRoomReq.class);

        try {
            // 2) 비즈니스 로직 실행 → RoomSession
            RoomSession session = roomMgr.createRoom(cr, this.user);
            this.curRoom = session;

            // 3) RoomInfo DTO로 매핑 (생성자 인자 개수와 순서 주의!)
            var meta = session.getMeta();
            RoomInfo info = new RoomInfo(meta, session.getMembers().size());

            // 4) 응답
            sendAck(PacketType.ACK, mapper.writeValueAsString(info));
        } catch (Exception e) {
            sendError("Create room failed: " + e.getMessage());
        }
    }

    /** 비공개 방 입장 */
    private void handleJoinPrivate(Packet pkt) throws IOException {
        var req = mapper.readTree(pkt.payloadJson());
        String roomId = req.get("roomId").asText();
        String password = req.get("password").asText();

        try {
            RoomSession session = roomMgr.joinPrivate(roomId, password, this);
            this.curRoom = session;
            sendAck(PacketType.ACK, "");
        } catch (Exception e) {
            sendError("Private join failed: " + e.getMessage());
        }
    }

    /** 타이머 시작 요청 처리 */
    private void handleTimerStart(Packet pkt) {
        if (curRoom == null) return;
        if (pkt.type() == PacketType.TIMER_FOCUS_START) {
            curRoom.startFocus();
        } else {
            curRoom.startBreak();
        }
    }

    /** 채팅 메시지 저장 및 브로드캐스트 */
    private void handleChat(Packet pkt) throws IOException {
        if (curRoom == null) return;
        // payloadJson 예시: {"text":"Hello!"}
        String text = mapper.readTree(pkt.payloadJson())
                .get("text")
                .asText();
        curRoom.broadcast(pkt);
    }

    /** 로비로 복귀 */
    private void handleBackToLobby(Packet pkt) {
        if (curRoom != null) {
            curRoom.removeMember(this);
            curRoom = null;
        }
        sendAck(PacketType.ACK, "");
    }

    /** ACK 응답 전송 헬퍼 */
    private void sendAck(PacketType type, String body) {
        send(PacketType.ACK, body);
    }

    /** ERROR 응답 전송 헬퍼 */
    private void sendError(String msg) {
        send(PacketType.ERROR, "{\"message\":\"" + msg + "\"}");
    }

    /** JSON 직렬화하여 클라이언트로 전송 */
    private void send(PacketType type, String body) {
        try {
            String json = mapper.writeValueAsString(new Packet(type, body));
            out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * MessageBroadcaster 에서 방에 속한 모든 클라이언트로
     * 비동기 브로드캐스트할 때 호출
     *
     * @param pkt 전송할 Packet 객체
     * @throws Exception 직렬화 또는 네트워크 오류 시
     */
    public void sendPacket(Packet pkt) throws Exception {
        String json = mapper.writeValueAsString(pkt);
        out.println(json);
        out.flush();
    }
}
