package com.studybuddy.client.ui;

import com.studybuddy.client.model.RoomInfo;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

import java.io.PrintWriter;

public class RoomListController {
    @FXML private TableView<RoomInfo> roomTable;
    @FXML private TableColumn<RoomInfo, String> nameCol, statusCol;
    @FXML private TableColumn<RoomInfo, Integer> curCol, maxCol;
    @FXML private Button refreshButton, joinButton, backButton;
    @FXML private Text errorText;

    private ObservableList<RoomInfo> rooms = FXCollections.observableArrayList();
    private PrintWriter out;

    @FXML
    public void initialize() {
        roomTable.setItems(rooms);
        refreshButton.setOnAction(e -> loadRooms());
        joinButton.setOnAction(e -> joinSelected());
        backButton.setOnAction(e -> goBack());
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    private void loadRooms() {
        try {
            Packet pkt = new Packet(PacketType.LIST_ROOMS, "");
            out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(pkt));
            // TODO: 서버 응답 처리 → rooms.setAll(...)
        } catch (Exception ex) {
            errorText.setText("목록 조회 실패");
        }
    }

    private void joinSelected() {
        RoomInfo sel = roomTable.getSelectionModel().getSelectedItem();
        if (sel == null) { errorText.setText("방을 선택하세요"); return; }
        // TODO: JOIN_ROOM 패킷 전송 & 응답 수신 → StudyRoomController 로 전환
    }

    private void goBack() {
        // TODO: LobbyController 로 장면 전환
    }
}
