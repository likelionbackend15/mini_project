package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.studybuddy.common.util.JsonUtil;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.model.UserSession;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;

import com.studybuddy.common.domain.Room;                        // ← 메타 저장용
import com.fasterxml.jackson.databind.node.ArrayNode;            // ← Jackson ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode;           // ← Jackson ObjectNode

/**
 * 방장 대기 화면 컨트롤러
 * - CREATE_ROOM, ROOM_UPDATE 패킷을 통해 참가자 목록 갱신
 * - TIMER_FOCUS_START 패킷 전송으로 포커드 시작 명령
 */
public class RoomHostController implements PacketListener {
    @FXML private Label roomNameLabel, statusLabel, countLabel;
    @FXML private TableView<Member> membersTable;
    @FXML private TableColumn<Member, String> nameCol, roleCol;
    @FXML private Button startButton;

    private final ObservableList<Member> members = FXCollections.observableArrayList();
    private MainApp app;
    private PrintWriter out;       // ← 클라이언트로 보낼 아웃풋
    private String roomId;
    private int maxMembers;

    private Room meta;            // CREATE_ROOM 응답 때 저장해 둘 메타데이터

    private static final Logger log = LoggerFactory.getLogger(RoomHostController.class);

    @FXML
    public void initialize() {
        // 테이블 바인딩
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        roleCol.setCellValueFactory(c -> c.getValue().roleProperty());
        membersTable.setItems(members);

        // 클릭 시 서버에 START 요청
        startButton.setOnAction(e -> sendStart());
    }

    /** MainApp 에서 주입됩니다. */
    public void setApp(MainApp app) {
        this.app = app;
        app.addScreenListener(this);
    }

    /** MainApp 에서 주입됩니다. */
    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void onPacket(Packet pkt) {
        try {
            // 1) 뽑아둘 JSON 트리
            JsonNode root = JsonUtil.mapper().readTree(pkt.payloadJson());

            // 2) 방 생성 / 업데이트 응답
            if (pkt.type()==PacketType.ACK) {
                String action = root.path("action").asText();
                if ("CREATE_ROOM".equals(action) || "ROOM_UPDATE".equals(action)) {
                    // info → meta 추출
                    JsonNode info = root.get("info");
                    meta = JsonUtil.mapper().treeToValue(info.get("meta"), Room.class);
                    Platform.runLater(() -> updateRoomInfo(root));
                }
            }
            // 3) 타이머 시작 브로드캐스트 오면 화면 전환
            else if (pkt.type() == PacketType.TIMER_FOCUS_START) {
                Platform.runLater(() ->
                        app.forwardTo("/fxml/PomodoroFocusView.fxml", pkt)
                );
            }
        } catch (Exception ex) {
            log.error("패킷 처리 오류", ex);
        }
    }

    private void updateRoomInfo(JsonNode root) {
        // 1) info → meta / curMembers 분리
        JsonNode info = root.path("info");
        JsonNode meta = info.path("meta");

        roomId        = meta.path("roomId").asText();
        roomNameLabel.setText(meta.path("name").asText());

        // 2) 최대인원은 meta.maxMembers
        maxMembers = meta.path("maxMembers").asInt();

        // 3) 상태 표시 (OPEN, LOCKED 등)
        statusLabel.setText(meta.path("status").asText());

        // 4) 멤버 테이블 리셋 및 방장 추가
        members.clear();
        var current = UserSession.getInstance().getCurrentUser();
        members.add(new Member(
                current.getId(),
                current.getUsername(),     // User 도메인에 있는 닉네임
                "Host"
        ));

        // 5) 서버가 보내준 members 배열(ROOM_UPDATE 시에만 있을 수 있음)
        for (JsonNode u : root.withArray("members")) {
            String uid = u.path("id").asText();
            if (uid.equals(current.getId())) continue;  // 방장은 이미 넣었으므로 skip
            String uname = u.path("name").asText();
            // role 결정 (meta.hostId 와 비교)
            String role = uid.equals(meta.path("hostId").asText()) ? "Host" : "Member";
            members.add(new Member(uid,uname, role));
        }

        // 6) 참가자 수: info.curMembers / meta.maxMembers
        int curCount = info.path("curMembers").asInt();
        countLabel.setText("Participants: " + curCount + " / " + maxMembers);

        // 7) Start 버튼: 방장이라면 언제나 활성화
        boolean iAmHost = current.getId().equals(meta.path("hostId").asText());
        startButton.setDisable(!iAmHost);
    }



    /** 집중 시작 명령 전송 */
    private void sendStart() {
        try {
            // 1) 메타에서 포커스·브레이크 시간, 루프 수 뽑아오기
            int focusSec = meta.getFocusMin() * 60;
            int breakSec = meta.getBreakMin() * 60;
            int loops    = meta.getLoops();

            // 2) 멤버 리스트 JSON으로 만들기
            ArrayNode membersArray = JsonUtil.mapper().createArrayNode();
            for (Member m : members) {
                membersArray.add(
                        JsonUtil.mapper().createObjectNode()
                                .put("id",   m.idProperty().get())
                                .put("name", m.nameProperty().get())
                                .put("role", m.roleProperty().get())
                );
            }

            // 3) 초기 payload 조립
            ObjectNode init = JsonUtil.mapper().createObjectNode();
            init.put("roomId",       meta.getRoomId());
            init.put("focusSec",     focusSec);
            init.put("breakSec",     breakSec);
            init.put("totalLoops",   loops);
            init.put("loopIdx",      1);            // 첫 사이클
            init.put("remainingSec", focusSec);
            init.set("members",      membersArray);

            // ───────────────────────────────────────────────────────────────
            // 4) 화면 전환: PomodoroFocusView.fxml 로 init 데이터 전달
            Packet initPkt = new Packet(
                    PacketType.TIMER_FOCUS_START,
                    init.toString()
            );
            app.forwardTo("/fxml/PomodoroFocusView.fxml", initPkt);

            // 5) 서버에도 시작 명령 전송 (서버에서 실제 타이머 브로드캐스트를 받기 위해)
            Packet startCmd = new Packet(
                    PacketType.TIMER_FOCUS_START,
                    String.format("{\"roomId\":\"%s\"}", roomId)
            );
            out.println(JsonUtil.mapper().writeValueAsString(startCmd));

        } catch (Exception ex) {
            log.error("Start Focus 전송 실패", ex);
        }
    }



    @Override
    public void onError(Exception e) {
        log.error("네트워크 오류", e);
    }

    /** TableView 모델 */
    public static class Member {
        private final javafx.beans.property.SimpleStringProperty id;    // ← 추가
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty role;

        // 기존 생성자 대신 id까지 받도록 변경
        public Member(String id, String name, String role) {
            this.id   = new javafx.beans.property.SimpleStringProperty(id);
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.role = new javafx.beans.property.SimpleStringProperty(role);
        }

        // id 접근자 추가
        public javafx.beans.property.StringProperty idProperty() {
            return id;
        }

        public javafx.beans.property.StringProperty nameProperty() {
            return name;
        }
        public javafx.beans.property.StringProperty roleProperty() {
            return role;
        }
    }

}
