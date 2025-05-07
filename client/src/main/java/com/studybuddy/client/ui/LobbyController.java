package com.studybuddy.client.ui;

import com.studybuddy.client.MainApp;
import com.studybuddy.client.model.UserSession;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

import java.io.PrintWriter;

public class LobbyController {
    @FXML
    private Text welcomeText;
    @FXML
    private Button createRoomButton;
    @FXML
    private Button listRoomsButton;
    @FXML
    private Button statsButton;
    @FXML
    private Button joinPrivateRoomButton;

    private PrintWriter out;
    private MainApp app;

    @FXML
    public void initialize() {
        if (UserSession.getInstance().getCurrentUser() != null) {
            welcomeText.setText("환영합니다, " + UserSession.getInstance().getCurrentUser().getUsername());
        } else {
            welcomeText.setText("환영합니다, 사용자님");
            System.err.println("⚠ UserSession에 사용자 정보가 없습니다.");
        }
        // 버튼 클릭 이벤트 설정
        createRoomButton.setOnAction(e -> showCreateRoom());
        listRoomsButton.setOnAction(e -> showRoomList());
        statsButton.setOnAction(e -> requestStats());
        joinPrivateRoomButton.setOnAction(e -> joinPrivateRoom());
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    public void setApp(MainApp app) {
        this.app = app;
    }

    private void showCreateRoom() {
        // TODO: FXML 로딩 & 장면 전환
        Platform.runLater(() ->
                app.forwardTo("/fxml/RoomCreateView.fxml", null));
    }

    private void showRoomList() {
        // TODO: 방 목록 요청 → RoomListController 로 전환
        Platform.runLater(() ->
                app.forwardTo("/fxml/RoomHostView.fxml", null));
    }

    private void joinPrivateRoom() {
        // TODO: 비공개방 입장 요청
        Platform.runLater(() ->
                app.forwardTo("/fxml/PrivateRoomJoinView.fxml", null));
    }

    private void requestStats() {
        try {
            Packet pkt = new Packet(PacketType.STATS_VIEW, "");
            out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(pkt));
            // TODO: 응답 수신 → StatsController 로 전환
            Platform.runLater(() ->
                    app.forwardTo("/fxml/MyInfoView.fxml", null));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
