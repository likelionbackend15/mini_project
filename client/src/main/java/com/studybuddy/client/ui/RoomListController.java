package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.dto.RoomInfo;
import com.studybuddy.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.io.PrintWriter;
import java.util.Arrays;

public class RoomListController implements PacketListener {
    @FXML private TableView<RoomInfo> roomTable;
    @FXML private TableColumn<RoomInfo, String> nameCol;
    @FXML private TableColumn<RoomInfo, String> statusCol;
    @FXML private TableColumn<RoomInfo, Integer> curCol, maxCol, loopsCol;
    @FXML private TableColumn<RoomInfo, Boolean> midEntryCol;
    @FXML private TableColumn<RoomInfo, String> hostIdCol;
    @FXML private Button refreshButton, joinButton, backButton;
    @FXML private Text errorText;

    private final ObservableList<RoomInfo> rooms = FXCollections.observableArrayList();
    private PrintWriter out;
    private MainApp app;
    private final ObjectMapper mapper = JsonUtil.mapper();

    @FXML
    public void initialize() {
        roomTable.setItems(rooms);

        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        curCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCurMembers()).asObject());
        maxCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getMaxMembers()).asObject());
        loopsCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getLoops()).asObject());
        midEntryCol.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().isAllowMidEntry()).asObject());
        hostIdCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHostId()));

        refreshButton.setOnAction(e -> loadRooms());
        joinButton.setOnAction(e -> joinSelected());
        backButton.setOnAction(e -> goBack());

        errorText.setVisible(false);
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    public void setApp(MainApp app) {
        this.app = app;
        app.addScreenListener(this);
    }

    private void loadRooms() {
        errorText.setVisible(false);
        try {
            Packet pkt = new Packet(PacketType.LIST_ROOMS, "");
            String json = mapper.writeValueAsString(pkt);
            out.println(json);
        } catch (Exception ex) {
            errorText.setText("목록 조회 실패: " + ex.getMessage());
            errorText.setVisible(true);
        }
    }

    private void joinSelected() {
        RoomInfo sel = roomTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            errorText.setText("입장할 방을 선택하세요.");
            errorText.setVisible(true);
            return;
        }

        try {
            Packet pkt = new Packet(PacketType.JOIN_ROOM,
                    String.format("{\"roomId\":\"%s\"}", sel.getRoomId()));
            out.println(mapper.writeValueAsString(pkt));
        } catch (Exception e) {
            errorText.setText("입장 요청 실패");
            errorText.setVisible(true);
        }
    }

    private void goBack() {
        Platform.runLater(() -> app.forwardTo("/fxml/LobbyView.fxml", null));
    }

    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() != PacketType.ACK) return;

        Platform.runLater(() -> {
            try {
                JsonNode root = mapper.readTree(pkt.payloadJson());

                if (root.isArray()) {
                    RoomInfo[] list = mapper.treeToValue(root, RoomInfo[].class);
                    rooms.setAll(Arrays.asList(list));
                    errorText.setVisible(false);
                }

            } catch (Exception e) {
                errorText.setText("응답 처리 오류: " + e.getMessage());
                errorText.setVisible(true);
            }
        });
    }

    @Override
    public void onError(Exception e) {
        Platform.runLater(() -> {
            errorText.setText("네트워크 오류: " + e.getMessage());
            errorText.setVisible(true);
        });
    }
}
