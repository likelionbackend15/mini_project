package com.studybuddy.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.domain.ChatMessage;
import com.studybuddy.common.domain.Room;
import com.studybuddy.common.domain.User;
import com.studybuddy.common.dto.CreateRoomReq;
import com.studybuddy.common.dto.RoomInfo;
import com.studybuddy.common.dto.RoomInitResponse;
import com.studybuddy.common.dto.RoomStats; // ★ 통계 DTO (있다고 가정)

import com.studybuddy.common.util.JsonUtil;
import com.studybuddy.server.dao.LogDAO;

import com.studybuddy.server.dao.UserDAO;
import com.studybuddy.server.util.MailUtil;
import jakarta.mail.MessagingException;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
   private final LogDAO logDao;

    private BufferedReader in;
    private PrintWriter    out;

    private User        user;     // 로그인 성공 시 채워짐
    private RoomSession curRoom;  // 현재 들어가 있는 방

    /* ===== 비밀번호 재설정용 인증 코드 ===== */
    private record CodeInfo(String code, LocalDateTime expires) {}
    private static final Map<String, CodeInfo> codes = new ConcurrentHashMap<>();

    public ClientHandler(Socket s, UserDAO uDao, RoomManager rMgr) {
        this.socket  = s;
        this.userDao = uDao;
        this.roomMgr = rMgr;
        this.logDao  = new LogDAO();
    }

    /* ---------------- 메인 루프 ---------------- */
    @Override public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    Packet pkt = mapper.readValue(line, Packet.class);
                    dispatch(pkt);                 // ★ 실제 처리
                } catch (Exception ex) {            // ← dispatch 내부 예외 잡기
                    log.error("패킷 처리 실패", ex);   // ① 스택 출력
                    sendError("Server error: " + ex.getMessage());
                }
            }
        } catch (Exception e) {                    // 네트워크 오류 등
            log.error("연결 종료(예외)", e);          // ② 스택 출력
        } finally {
            cleanup();
        }
    }


    /* ---------------- Packet 라우팅 ---------------- */
    private void dispatch(Packet p) throws IOException, SQLException, MessagingException {
        log.debug("recv={}", p.type());

        switch (p.type()) {
            /* 회원 */
            case SIGNUP              -> handleSignup(p);
            case LOGIN               -> handleLogin(p);
            case SEND_CODE             -> handleSendCode(p);
            case RESET_PASSWORD        -> handleResetPw(p);
            case DELETE_ACCOUNT       -> handleDeleteAccount(p);

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
        // JSON 문자열 → Tree
        var req = mapper.readTree(p.payloadJson());

        // payload 에 담긴 id, username, password, email, code 꺼내기
        String id       = req.get("id").asText();        // 로그인 ID
        String username = req.get("username").asText();  // 닉네임
        String pwPlain  = req.get("password").asText();
        String email    = req.get("email").asText(""); // 없으면 빈 문자열
        String code     = req.get("code").asText();      // 인증 코드

        try {

            /* 1) 중복 검사 */
            if (userDao.existsById(id)) {          // ← PK 중복 체크
                sendError("ID already exists");
                return;
            }
            if (userDao.findByEmail(email).isPresent()) {
                sendError("Email already used"); return; }

            /* 2) 코드 검증 */
            CodeInfo info = codes.get(email);

            if (info==null || !info.code.equals(code) ||
                    LocalDateTime.now().isAfter(info.expires)) {
                sendError("Code invalid or expired");
                return;
            }
            codes.remove(email);   // 1회용

            /* 3) 저장 */
            String hash = BCrypt.hashpw(pwPlain, BCrypt.gensalt());
            User u = new User(id, username, hash, null, email);
            userDao.save(u);
            sendAck("{\"action\":\"SIGNUP\"}"); // 성공 응답으로 User JSON 보내기
        } catch (Exception e) {
            sendError("Signup failed: " + e.getMessage());
        }
    }


    /** 로그인 */
    private void handleLogin(Packet p) throws IOException, SQLException {
        // JSON 문자열 → Tree
        var req = mapper.readTree(p.payloadJson());

        // payload 에 담긴 id, password 꺼내기
        String id = req.get("id").asText();     // 로그인 ID
        String password = req.get("password").asText();

        Optional<User> opt = userDao.findById(id);
        if (opt.isPresent() && BCrypt.checkpw(password, opt.get().getHashedPw())) {
            user = opt.get();
            sendAck("{\"action\":\"LOGIN\", \"user\":" +
                    mapper.writeValueAsString(user) + "}");
        } else {
            sendError("Invalid credentials");
        }
    }

    /* 인증 코드 전송 */
    private void handleSendCode(Packet p) throws IOException, SQLException {

        String email = mapper.readTree(p.payloadJson()).get("email").asText();
        log.debug("STEP-1  email={}", email);   // ① 도착 확인

        if (userDao.findByEmail(email).isPresent()) {
            log.debug("STEP-1a 중복 이메일");    // (중복이라면 여기까지만 찍힘)
            sendError("Email already used");
            return;
        }

        String code = String.format("%06d", (int)(Math.random()*1_000_000));
        codes.put(email, new CodeInfo(code, LocalDateTime.now().plusMinutes(10)));
        log.debug("STEP-2  code 생성={}", code); // ② 코드 생성까지 통과 ---------

        try {
            MailUtil.sendCode(email, code);          // 실제 메일
            log.debug("STEP-3  MailUtil OK");      // ③ 메일 전송 성공 ----------
        } catch (Exception ex) {                     // ★ SMTP 실패
            log.warn("메일 전송 실패, 코드 = {}", code, ex);
            // dev 모드에서는 메일 대신 콘솔 출력만으로도 충분
        }

        // 메일 성공/실패와 무관하게 ACK 전달하여 UI 피드백
        sendAck("{\"action\":\"SEND_CODE\"}");
        log.debug("STEP-4  ACK 보냄");            // ④ 최종 도달 -----------------
    }


    /* 비밀번호 재설정 */
    private void handleResetPw(Packet p) throws IOException {
        var j = mapper.readTree(p.payloadJson());
        String email = j.get("email").asText();
        String code  = j.get("code").asText();
        String newPw = j.get("newPw").asText();

        CodeInfo info = codes.get(email);
        if (info == null || !info.code.equals(code) ||
                LocalDateTime.now().isAfter(info.expires)) {
            sendError("인증 코드가 잘못됐거나 만료되었습니다");
            return;
        }

        String hash = BCrypt.hashpw(newPw, BCrypt.gensalt());

        try {
            Optional<User> userOpt = userDao.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("[DEBUG] 비밀번호 변경 시도 - id: " + user.getId());

                userDao.updatePassword(user.getId(), hash);
                log.debug("updatePassword 완료: id={}", user.getId());
                codes.remove(email);

                sendAck("{\"action\":\"RESET_OK\"}");
            } else {
                sendError("등록된 이메일이 없습니다");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError("비밀번호 변경 실패: " + e.getMessage());
        }
    }

    /* 계정 삭제 */

    private void handleDeleteAccount(Packet p) throws IOException, SQLException {
        // JSON 문자열 → Tree
        var req = mapper.readTree(p.payloadJson());

        // payload 에 담긴 id 꺼내기
        String id = req.get("id").asText();
        String password = req.get("password").asText();
        // 사용자 존재 여부 확인
        Optional<User> opt = userDao.findById(id);
        if (opt.isPresent() && BCrypt.checkpw(password, opt.get().getHashedPw())) {
            boolean deleted = userDao.deleteAccount(id);
            if (deleted) {
                sendAck("{\"action\":\"DELETE_ACCOUNT\"}");
            } else {
                sendError("계정 삭제에 실패했습니다.");
            }
        } else {
            sendError("계정과 비밀번호를 다시 확인해주세요.");
        }
    }

    /* =================================================
       2) 로비 / 방
    ================================================= */

    /** 로비 방 목록 */
    private void handleListRooms() {
        try {
            List<RoomInfo> list = roomMgr.listOpenRooms();
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

                                // ——— 과거 채팅 불러오기 ———
                                        List<ChatMessage> history =
                                    logDao.findMessagesByRoom(curRoom.getMeta().getRoomId());

                                RoomInitResponse init = new RoomInitResponse(
                                  curRoom.getMeta().getRoomId(),
                                  curRoom.getMembers().size(),
                                  curRoom.getMeta().getMaxMembers(),
                                  curRoom.getMeta().getLoops(),
                                  curRoom.getMeta().getStatus().name(),
                                  curRoom.getMeta().isAllowMidEntry(),
                                  curRoom.getMeta().getHostId(),
                                  history
                                        );
                        // 클라이언트 단독 전송
                                sendPacket(new Packet(
                                          PacketType.ROOM_INIT,
                                          mapper.writeValueAsString(init)
                                                ));
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

        private void handleChat(Packet p) {
              if (curRoom == null) return;
              try {
                    // 1) JSON 파싱
                            var node   = mapper.readTree(p.payloadJson());
                    String rid = node.get("roomId").asText();
                    String snd = node.get("sender").asText();
                    String txt = node.get("text").asText();

                            // 2) DB에 저장
                                    ChatMessage msg = new ChatMessage(
                                null, rid, snd, txt, LocalDateTime.now());
                    logDao.saveChat(msg);

                            // 3) 브로드캐스트
                                    curRoom.broadcast(p);
                  } catch (Exception ex) {
                    log.error("CHAT 처리 실패", ex);
                  }
            }

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
    private void sendError(String m) {
        log.warn("sendError → {}", m);             // ③ 서버 로그에도 남기기
        send(PacketType.ERROR, "{\"message\":\""+m+"\"}");
    }

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
