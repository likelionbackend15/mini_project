package com.studybuddy.client.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
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

        // 1) 맨 처음엔 항상 비활성화
        startButton.setDisable(true);

        // 2) 클릭 시 서버에 START 요청
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
        log.debug("RoomHostController.onPacket() 입장: pkt.type={} payload={}", pkt.type(), pkt.payloadJson());

        try {
            JsonNode root = JsonUtil.mapper().readTree(pkt.payloadJson());

            if (pkt.type() == PacketType.ACK) {
                String action = root.path("action").asText();
                if ("CREATE_ROOM".equals(action) || "ROOM_UPDATE".equals(action)) {
                    // 1) info 노드 꺼내기
                    JsonNode info = root.get("info");
                    if (info == null) {
                        log.error("no 'info' field in ACK payload!");
                        return;
                    }

                    // 한 번에 Room 으로 파싱
                    meta = JsonUtil.mapper().treeToValue(info, Room.class);

                    Platform.runLater(() -> {
                        try {
                            updateRoomInfo(info);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
            // 3) 타이머 시작 브로드캐스트 오면 화면 전환
            else if (pkt.type() == PacketType.TIMER_FOCUS_START) {
                Platform.runLater(() -> {
                    try {
                        // 1) FXML 직접 로드
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/fxml/PomodoroView.fxml")
                        );
                        Parent view = loader.load();    // ← 변수명을 root → view 로 변경

                        // 2) 컨트롤러 얻어서 의존성 주입
                        PomodoroController ctrl = loader.getController();
                        ctrl.setWriter(out);
                        app.addScreenListener(ctrl);

                        // 3) 처음 받은 TIMER_FOCUS_START 패킷 전달 (init)
                        ctrl.onPacket(pkt);

                        // 4) 씬 전환
                        Stage stage = (Stage) startButton.getScene().getWindow();
                        stage.setScene(new Scene(view));  // ← view 사용
                    } catch (Exception e) {
                        log.error("Pomodoro 뷰 전환 실패", e);
                    }
                });
            }

        } catch (Exception ex) {
            log.error("패킷 처리 오류", ex);
        }
    }

    private void updateRoomInfo(JsonNode info) throws JsonProcessingException {
        // 1) info 전체를 Room 객체로 바로 파싱
        this.meta = JsonUtil.mapper().treeToValue(info, Room.class);

        // 2) roomId, roomName
        this.roomId = info.path("roomId").asText();
        roomNameLabel.setText(info.path("name").asText());

        // 3) 최대인원 저장
        this.maxMembers = info.path("maxMembers").asInt();

        // 4) 상태 표시
        statusLabel.setText(info.path("status").asText());

        // 5) 참가자 목록 초기화 및 방장 추가
        members.clear();
        var current = UserSession.getInstance().getCurrentUser();
        members.add(new Member(
                current.getId(),
                current.getUsername(),
                "Host"
        ));

        // 6) 서버가 내려준 members 배열
        for (JsonNode u : info.withArray("members")) {
            String uid = u.path("id").asText();
            if (uid.equals(current.getId())) continue;
            String name = u.path("name").asText();
            String role = u.path("role").asText();
            members.add(new Member(uid, name, role));
        }

        // 7) 참가자 수: curMembers / maxMembers
        int curCount = info.path("curMembers").asInt();
        countLabel.setText("Participants: " + curCount + " / " + this.maxMembers);

        // 8) 버튼 활성화 (호스트만)
        boolean iAmHost = current.getId().equals(info.path("hostId").asText());
        startButton.setDisable(!iAmHost);
    }





    /** 집중 시작 명령 전송 */
    private void sendStart() {

        if (meta == null) {
            log.warn("meta 아직 없음—타이머 못 시작");
            return;
        }

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
            app.forwardTo("/fxml/PomodoroView.fxml", initPkt);

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
