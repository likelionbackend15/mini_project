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
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        curCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCurMembers()).asObject());
        maxCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getMaxMembers()).asObject());
        loopsCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getLoops()).asObject());
        midEntryCol.setCellValueFactory(cell -> new SimpleBooleanProperty(cell.getValue().isAllowMidEntry()).asObject());
        hostIdCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getHostId()));

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
            out.println(mapper.writeValueAsString(pkt));
        } catch (Exception e) {
            showError("목록 조회 실패: " + e.getMessage());
        }
    }

    private void joinSelected() {
        RoomInfo selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("입장할 방을 선택하세요.");
            return;
        }

        try {
            if (selected.getName().startsWith("🔒")) {
                // 비공개 방인 경우 팝업 열기
                openPasswordPopup(selected);
            } else if (selected.isAllowMidEntry()) {
                // 공개 + 중간입장 허용이면 바로 입장
                Packet pkt = new Packet(PacketType.JOIN_ROOM,
                        String.format("{\"roomId\":\"%s\"}", selected.getRoomId()));
                out.println(mapper.writeValueAsString(pkt));
            } else {
                showError("이 방은 중간 입장이 허용되지 않습니다.");
            }
        } catch (Exception e) {
            showError("입장 요청 실패: " + e.getMessage());
        }
    }

    private void openPasswordPopup(RoomInfo room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PrivateRoomJoinPopup.fxml"));
            Parent root = loader.load();

            PrivateRoomJoinPopupController controller = loader.getController();
            controller.setRoomId(room.getRoomId());
            controller.setWriter(out);

            Stage stage = new Stage();
            stage.setTitle("비밀번호 입력");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            showError("팝업 열기 실패: " + e.getMessage());
        }
    }

    private void goBack() {
        Platform.runLater(() -> app.forwardTo("/fxml/LobbyView.fxml", null));
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorText.setVisible(true);
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
                showError("응답 처리 오류: " + e.getMessage());
            }
        });
    }

    @Override
    public void onError(Exception e) {
        Platform.runLater(() -> showError("네트워크 오류: " + e.getMessage()));
    }
}
