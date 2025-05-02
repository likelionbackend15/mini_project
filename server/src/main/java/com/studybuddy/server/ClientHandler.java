package com.studybuddy.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.domain.Room;
import com.studybuddy.common.domain.User;
import com.studybuddy.common.dto.CreateRoomReq;
import com.studybuddy.common.dto.RoomInfo;
import com.studybuddy.common.dto.RoomStats; // ★ 통계 DTO (있다고 가정)
import com.studybuddy.server.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** 1명의 클라이언트와 통신하는 스레드 */
public class ClientHandler implements Runnable {

    /* ---------------- 필드 ---------------- */
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Socket socket;
    private final UserDAO userDao;
    private final RoomManager roomMgr;

    private BufferedReader in;
    private PrintWriter    out;

    private User        user;     // 로그인 성공 시 채워짐
    private RoomSession curRoom;  // 현재 들어가 있는 방

    public ClientHandler(Socket s, UserDAO uDao, RoomManager rMgr) {
        this.socket  = s;
        this.userDao = uDao;
        this.roomMgr = rMgr;
    }

    /* ---------------- 메인 루프 ---------------- */
    @Override public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                Packet pkt = mapper.readValue(line, Packet.class);
                dispatch(pkt);
            }
        } catch (IOException | SQLException e) {
            log.info("연결 종료: {}", socket.getRemoteSocketAddress());
        } finally {
            cleanup();
        }
    }

    /* ---------------- Packet 라우팅 ---------------- */
    private void dispatch(Packet p) throws IOException, SQLException {
        switch (p.type()) {
            /* 회원 */
            case SIGNUP              -> handleSignup(p);
            case LOGIN               -> handleLogin(p);

            /* 로비 */
            case LIST_ROOMS          -> handleListRooms();
            case CREATE_ROOM         -> handleCreateRoom(p);
            case JOIN_ROOM           -> handleJoinRoom(p);
            case JOIN_PRIVATE        -> handleJoinPrivate(p);
            case BACK_TO_LOBBY       -> handleBackToLobby(p);

            /* 방 설정 */
            case MODIFY_ROOM         -> handleModifyRoom(p);
            case LOCK_ROOM           -> handleLockRoom(p);

            /* 타이머 */
            case TIMER_FOCUS_START,
                 TIMER_BREAK_START   -> handleTimerStart(p);

            /* 채팅 */
            case CHAT                -> handleChat(p);

            /* 통계 / CSV */
            case ROOM_STATS, STATS_VIEW  -> handleRoomStats(p);
            case DOWNLOAD_CSV            -> handleDownloadCsv(p);

            /* 그 외 */
            default -> sendError("Unknown packet: " + p.type());
        }
    }

    /* =================================================
       1) 회원 영역
    ================================================= */

    /** 회원가입 */
    private void handleSignup(Packet p) throws IOException {
        var req = mapper.readTree(p.payloadJson());
        String username = req.get("username").asText();
        String pwPlain  = req.get("password").asText();
        String email    = req.get("email").asText(""); // 없으면 빈 문자열

        try {
            if (userDao.existsByUsername(username)) {  // 중복 검사는 existsByUsername 사용
                sendError("Username already exists");
                return;
            }
            String hash = BCrypt.hashpw(pwPlain, BCrypt.gensalt());

            User u = new User(
                    null,          // id는 DB에서 자동 생성
                    username,
                    hash,
                    null,          // createdAt : INSERT 시 CURRENT_TIMESTAMP
                    email
            );
            long id = userDao.save(u);   // DB INSERT
            u.setId(id);                 // 메모리 객체에도 id 세팅
            sendAck(mapper.writeValueAsString(u)); // 성공 응답으로 User JSON 보내기
        } catch (Exception e) {
            sendError("Signup failed: " + e.getMessage());
        }
    }


    /** 로그인 */
    private void handleLogin(Packet p) throws IOException, SQLException {
        var req = mapper.readTree(p.payloadJson());
        String username = req.get("username").asText();
        String password = req.get("password").asText();

        Optional<User> opt = userDao.findByUsername(username);
        if (opt.isPresent() && BCrypt.checkpw(password, opt.get().getHashedPw())) {
            user = opt.get();
            sendAck(mapper.writeValueAsString(user));
        } else {
            sendError("Invalid credentials");
        }
    }

    /* =================================================
       2) 로비 / 방
    ================================================= */

    /** 로비 방 목록 */
    private void handleListRooms() {
        try {
            List<Room> list = roomMgr.listOpenRooms();
            sendAck(mapper.writeValueAsString(list));
        } catch (Exception e) {
            sendError("List rooms failed: " + e.getMessage());
        }
    }

    /** 방 생성 */
    private void handleCreateRoom(Packet p) throws IOException {
        if (user == null) { sendError("Login first"); return; }
        CreateRoomReq dto = mapper.readValue(p.payloadJson(), CreateRoomReq.class);

        try {
            curRoom = roomMgr.createRoom(dto, user);
            curRoom.addMember(this);    // 방장 본인
            RoomInfo info = new RoomInfo(curRoom.getMeta(), curRoom.getMembers().size());
            sendAck(mapper.writeValueAsString(info));
        } catch (Exception e) {
            sendError("Create room failed: " + e.getMessage());
        }
    }

    /** 공개 방 입장 */
    private void handleJoinRoom(Packet p) throws IOException {
        var req = mapper.readTree(p.payloadJson());
        try {
            curRoom = roomMgr.joinRoom(req.get("roomId").asText(), this);
            sendAck("");
        } catch (Exception e) {
            sendError("Join failed: " + e.getMessage());
        }
    }

    /** 비공개 방 입장 */
    private void handleJoinPrivate(Packet p) throws IOException {
        var r = mapper.readTree(p.payloadJson());
        try {
            curRoom = roomMgr.joinPrivate(r.get("roomId").asText(),
                    r.get("password").asText(), this);
            sendAck("");
        } catch (Exception e) {
            sendError("Private join failed: " + e.getMessage());
        }
    }

    /** 로비 복귀 */
    private void handleBackToLobby(Packet p) {
        if (curRoom != null) {
            curRoom.removeMember(this);
            curRoom = null;
        }
        sendAck("");
    }

    /* =================================================
       3) 방 설정
    ================================================= */

    /** 방 정보 수정 */
    private void handleModifyRoom(Packet p) throws IOException {
        if (curRoom == null) { sendError("Not in room"); return; }

        Room updated = mapper.readValue(p.payloadJson(), Room.class); // DTO ≈ Domain 가정
        try {
            roomMgr.modifyRoom(updated);
            sendAck("");
        } catch (Exception e) {
            sendError("Modify failed: " + e.getMessage());
        }
    }

    /** 방 잠금 */
    private void handleLockRoom(Packet p) throws IOException {
        var req = mapper.readTree(p.payloadJson());
        try {
            roomMgr.lockRoom(req.get("roomId").asText());
            sendAck("");
        } catch (Exception e) {
            sendError("Lock failed: " + e.getMessage());
        }
    }

    /* =================================================
       4) 타이머 / 채팅
    ================================================= */

    private void handleTimerStart(Packet p) {
        if (curRoom == null) return;
        if (p.type() == PacketType.TIMER_FOCUS_START) curRoom.startFocus();
        else                                          curRoom.startBreak();
    }

    private void handleChat(Packet p) { if (curRoom != null) curRoom.broadcast(p); }

    /* =================================================
       5) 통계 / CSV
    ================================================= */

    /** 통계 조회 */
    private void handleRoomStats(Packet p) throws IOException {
        String roomId = mapper.readTree(p.payloadJson()).get("roomId").asText();
        try {
            RoomStats stats = roomMgr.computeStats(roomId);
            sendAck(mapper.writeValueAsString(stats));
        } catch (Exception e) {
            sendError("Stats failed: " + e.getMessage());
        }
    }

    /** CSV 다운로드: 서버가 만든 CSV(byte[])를 Base64로 보내기 */
    private void handleDownloadCsv(Packet p) throws IOException {
        var node   = mapper.readTree(p.payloadJson());
        String roomId = node.get("roomId").asText();

        // 날짜 범위(from, to)가 JSON에 들어왔다면 파싱, 없으면 null
        LocalDateTime from = node.hasNonNull("from")
                ? LocalDateTime.parse(node.get("from").asText())
                : null;
        LocalDateTime to   = node.hasNonNull("to")
                ? LocalDateTime.parse(node.get("to").asText())
                : null;

        try {
            byte[] csv = roomMgr.generateCsv(roomId, from, to);        // ← 파라미터 3개
            String b64 = Base64.getEncoder().encodeToString(csv);

            sendAck("{\"fileBase64\":\"" + b64 + "\"}");
        } catch (Exception e) {
            sendError("CSV download failed: " + e.getMessage());
        }
    }


    /* =================================================
       6) 공통 응답
    ================================================= */
    private void sendAck(String body) { send(PacketType.ACK, body); }
    private void sendError(String m)   { send(PacketType.ERROR, "{\"message\":\""+m+"\"}"); }

    private void send(PacketType t, String body) {
        try { out.println(mapper.writeValueAsString(new Packet(t, body))); }
        catch (Exception e) { log.error("send 실패", e); }
    }

    /* =================================================
       7) 브로드캐스트에서 호출
    ================================================= */
    public void sendPacket(Packet pkt) throws Exception {
        out.println(mapper.writeValueAsString(pkt));
        out.flush();
    }

    /* =================================================
       8) 정리
    ================================================= */
    private void cleanup() {
        try {
            if (curRoom != null) curRoom.removeMember(this);
            socket.close();
        } catch (IOException ignored) {}
    }

    public User getUser() { return user; }
}
