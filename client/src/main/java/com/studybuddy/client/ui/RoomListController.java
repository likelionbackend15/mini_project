package com.studybuddy.client.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.model.RoomInfo;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.io.PrintWriter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomListController implements PacketListener {

    @FXML private TableView<RoomInfo> roomTable;
    @FXML private TableColumn<RoomInfo, String> nameCol, statusCol;
    @FXML private TableColumn<RoomInfo, Integer> curCol, maxCol, loopsCol;
    @FXML private Button refreshButton, joinButton, backButton;
    @FXML private Text errorText;

    private ObservableList<RoomInfo> rooms = FXCollections.observableArrayList();
    private PrintWriter out;
    private MainApp app;

    private static final Logger log = LoggerFactory.getLogger(RoomListController.class);

    @FXML
    public void initialize() {
        roomTable.setItems(rooms);

        // 컬럼 설정
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        curCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getCurMembers()).asObject());
        maxCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getMaxMembers()).asObject());
        loopsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getLoops()).asObject());

        refreshButton.setOnAction(e -> loadRooms());
        joinButton.setOnAction(e -> joinSelected());
        backButton.setOnAction(e -> goBack());

        errorText.setText("");
    }

    public void setWriter(PrintWriter out) { this.out = out; }

    public void setApp(MainApp app) {
        this.app = app;
        app.addScreenListener(this);
        loadRooms(); // 초기 자동 조회
    }

    private void loadRooms() {
        try {
            Packet pkt = new Packet(PacketType.LIST_ROOMS, "");
            String jsonPkt = JsonUtil.mapper().writeValueAsString(pkt);
            out.println(jsonPkt);
            log.debug("LIST_ROOMS 요청: {}", jsonPkt);
        } catch (Exception ex) {
            errorText.setText("목록 조회 실패: " + ex.getMessage());
        }
    }

    private void joinSelected() {
        RoomInfo selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            errorText.setText("방을 선택하세요.");
            return;
        }

        try {
            PacketType type = selected.getStatus().equals("LOCKED") ? PacketType.JOIN_PRIVATE : PacketType.JOIN_ROOM;
            String payload = String.format("{\"roomId\":\"%s\"}", selected.getRoomId());

            Packet pkt = new Packet(type, payload);
            out.println(JsonUtil.mapper().writeValueAsString(pkt));
            log.debug("JOIN_ROOM 요청 보냄: {}", payload);
        } catch (Exception ex) {
            errorText.setText("방 입장 실패: " + ex.getMessage());
        }
    }

    private void goBack() {
        Platform.runLater(() ->
                app.forwardTo("/fxml/LobbyView.fxml", null));
    }

    @Override
    public void onPacket(Packet pkt) {
        Platform.runLater(() -> {
            if (pkt.type() == PacketType.ACK) handleAck(pkt);
            else if (pkt.type() == PacketType.ERROR) handleError(pkt);
        });
    }

    private void handleAck(Packet pkt) {
        try {
            if (pkt.payloadJson().trim().startsWith("[")) {  // 방 목록
                List<RoomInfo> roomList = JsonUtil.mapper()
                        .readValue(pkt.payloadJson(), new TypeReference<List<RoomInfo>>(){});
                rooms.setAll(roomList);
            } else {
                String action = JsonUtil.mapper().readTree(pkt.payloadJson()).path("action").asText();
                if ("JOIN_ROOM".equals(action) || "JOIN_PRIVATE".equals(action)) {
                    app.forwardTo("/fxml/StudyRoomView.fxml", pkt);
                }
            }
        } catch (Exception ex) {
            errorText.setText("응답 처리 오류: " + ex.getMessage());
        }
    }

    private void handleError(Packet pkt) {
        try {
            String message = JsonUtil.mapper().readTree(pkt.payloadJson()).path("message").asText();
            errorText.setText("오류: " + message);
        } catch (Exception e) {
            errorText.setText("알 수 없는 오류");
        }
    }

    @Override
    public void onError(Exception e) {
        Platform.runLater(() -> errorText.setText("네트워크 오류: " + e.getMessage()));
    }
}
