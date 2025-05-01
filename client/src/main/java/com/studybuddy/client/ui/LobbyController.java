package com.studybuddy.client.ui;

import com.studybuddy.client.model.UserSession;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

import java.io.PrintWriter;

public class LobbyController {
    @FXML private Text welcomeText;
    @FXML private Button createRoomButton;
    @FXML private Button listRoomsButton;
    @FXML private Button statsButton;

    private PrintWriter out;

    @FXML
    public void initialize() {
        welcomeText.setText("환영합니다, " + UserSession.getInstance().getCurrentUser().getUsername());
        createRoomButton.setOnAction(e -> showCreateRoom());
        listRoomsButton.setOnAction(e -> showRoomList());
        statsButton.setOnAction(e -> requestStats());
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    private void showCreateRoom() {
        // TODO: FXML 로딩 & 장면 전환
    }

    private void showRoomList() {
        // TODO: 방 목록 요청 → RoomListController 로 전환
    }

    private void requestStats() {
        try {
            Packet pkt = new Packet(PacketType.STATS_VIEW, "");
            out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(pkt));
            // TODO: 응답 수신 → StatsController 로 전환
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
