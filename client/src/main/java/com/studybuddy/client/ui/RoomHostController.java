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

/**
 * 방장 대기 화면 컨트롤러
 * - CREATE_ROOM, ROOM_UPDATE 패킷을 통해 참가자 목록 갱신
 * - TIMER_FOCUS_START 패킷 전송으로 포커드 시작 명령
 */
public class RoomHostController implements PacketListener {
    @FXML private Label roomNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    @FXML private TableView<Member> membersTable;
    @FXML private TableColumn<Member, String> nameCol;
    @FXML private TableColumn<Member, String> roleCol;
    @FXML private Button startButton;

    private final ObservableList<Member> members = FXCollections.observableArrayList();
    private MainApp app;
    private PrintWriter out;       // ← 클라이언트로 보낼 아웃풋
    private String roomId;
    private int maxMembers;

    private static final Logger log = LoggerFactory.getLogger(RoomHostController.class);

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        roleCol.setCellValueFactory(cell -> cell.getValue().roleProperty());
        membersTable.setItems(members);

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
        // Packet.type() 과 payloadJson() 사용
        if (pkt.type() != PacketType.ACK) return;
        try {
            JsonNode root = JsonUtil.mapper().readTree(pkt.payloadJson());
            String action = root.path("action").asText();

            // 방 생성 또는 참가자 업데이트
            if ("CREATE_ROOM".equals(action) || "ROOM_UPDATE".equals(action)) {
                Platform.runLater(() -> updateRoomInfo(root));

                // 서버가 집중 세션 시작을 승인했을 때
            } else if ("TIMER_FOCUS_START".equals(action)) {
                Platform.runLater(() ->
                        app.forwardTo("/fxml/PomodoroFocusView.fxml", pkt)
                );
            }
        } catch (Exception ex) {
            log.error("패킷 처리 오류", ex);
        }
    }

    private void updateRoomInfo(JsonNode root) {
        JsonNode info = root.get("info");
        roomId     = info.path("roomId").asText();
        roomNameLabel.setText(info.path("name").asText());
        maxMembers = info.path("maxMembers").asInt();

        statusLabel.setText(info.path("status").asText());

        members.clear();
        for (JsonNode u : root.withArray("members")) {
            String uid   = u.path("id").asText();
            String uname = u.path("name").asText();
            // getCurrentUser() 로 변경
            boolean isHost = uid.equals(UserSession.getInstance().getCurrentUser().getId());
            members.add(new Member(uname, isHost ? "Host" : "Member"));
        }
        countLabel.setText("Participants: " + members.size() + "/" + maxMembers);

        // 방장만, 최소 1명 이상일 때 버튼 활성화
        boolean iAmHost = UserSession.getInstance()
                .getCurrentUser()
                .getId()
                .equals(info.path("hostId").asText());
        startButton.setDisable(!(iAmHost && members.size() >= 1));
    }

    /** 집중 시작 명령 전송 */
    private void sendStart() {
        try {
            Packet startPkt = new Packet(
                    PacketType.TIMER_FOCUS_START,
                    String.format("{\"roomId\":\"%s\"}", roomId)
            );
            // out 필드를 통해 전송
            out.println(JsonUtil.mapper().writeValueAsString(startPkt));
        } catch (Exception ex) {
            log.error("TIMER_FOCUS_START 전송 실패", ex);
        }
    }

    @Override
    public void onError(Exception e) {
        log.error("네트워크 오류", e);
    }

    /** TableView 모델 */
    public static class Member {
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty role;
        public Member(String name, String role) {
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.role = new javafx.beans.property.SimpleStringProperty(role);
        }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty roleProperty() { return role; }
    }
}
