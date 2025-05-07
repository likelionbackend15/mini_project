package com.studybuddy.client.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.dto.RoomInfo;
import com.studybuddy.common.domain.Room;
import com.studybuddy.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;

import java.io.PrintWriter;

/**
 * 방장 전용 입장 화면 + 설정 패널까지 한 컨트롤러에서 관리
 */
public class RoomHostController implements PacketListener {
    private final ObjectMapper mapper = JsonUtil.mapper();
    private PrintWriter out;
    private MainApp app;

    private RoomInfo roomInfo;

    // --- 뷰 컴포넌트 ---
    @FXML private Label roomNameLabel, hostLabel, statusLabel, participantsLabel;
    @FXML private TableView<ParticipantRow> participantsTable;
    @FXML private TableColumn<ParticipantRow,String> nameCol, roleCol;
    @FXML private Button lockWaitButton, startFocusButton, editSettingsButton;

    // --- 설정 패널 컴포넌트 ---
    @FXML private Pane settingsOverlay;
    @FXML private Spinner<Integer> maxMembersSpinner;
    @FXML private Spinner<Integer> focusSpinner;
    @FXML private Spinner<Integer> breakSpinner;
    @FXML private Spinner<Integer> loopsSpinner;
    @FXML private CheckBox midEntryBox;
    @FXML private PasswordField passwordField;
    @FXML private Button applyButton;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        // 참가자 테이블 초기화
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        // 버튼 비활성
        lockWaitButton.setDisable(true);
        startFocusButton.setDisable(true);
        editSettingsButton.setDisable(true);

        // 설정 패널 초기화
        settingsOverlay.setVisible(false);
        initSpinners();
    }

    private void initSpinners() {
        maxMembersSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 20, 8));
        focusSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 120, 25));
        breakSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 5));
        loopsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 4));

        passwordField.setDisable(true);
        midEntryBox.selectedProperty().addListener((o,oldV,newV)->
                passwordField.setDisable(!newV));
        errorLabel.setVisible(false);
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }
    public void setApp(MainApp app) {
        this.app = app;
        app.addScreenListener(this);
    }

    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() == PacketType.ACK && roomInfo == null) {
            Platform.runLater(() -> {
                try {
                    // 1) wrapper 전체를 트리로 읽고
                    JsonNode root = mapper.readTree(pkt.payloadJson());
                    // 2) info 노드만 꺼내서
                    JsonNode infoNode = root.has("info") ? root.get("info") : root;
                    // 3) RoomInfo 로 역직렬화
                    roomInfo = mapper.treeToValue(infoNode, RoomInfo.class);
                    // 4) 화면 갱신
                    updateView();
                } catch (Exception ex) {
                    showError("방 정보 오류: " + ex.getMessage());
                }
            });
        }
        else if (pkt.type() == PacketType.ERROR) {
            Platform.runLater(() -> {
                String msg;
                try {
                    msg = mapper.readTree(pkt.payloadJson())
                            .path("message").asText();
                } catch (JsonProcessingException e) {
                    msg = "알 수 없는 오류";
                }
                showError(msg);
            });
        }
    }


    @Override
    public void onError(Exception e) {
        Platform.runLater(() -> showError("네트워크 오류: "+e.getMessage()));
    }

    private void updateView() {
        Room meta = roomInfo.getMeta();
        roomNameLabel.setText(meta.getName());
        hostLabel.setText("Host: "+meta.getHostId());
        statusLabel.setText(meta.getStatus().name());
        participantsLabel.setText(
                "Participants: "+roomInfo.getCurMembers()+" / "+meta.getMaxMembers()
        );

        participantsTable.getItems().setAll(
                new ParticipantRow(meta.getHostId(), "Host")
                // TODO: 실제 참가자들 추가
        );

        lockWaitButton.setDisable(false);
        startFocusButton.setDisable(false);
        editSettingsButton.setDisable(false);
    }

    @FXML
    private void onLockWait() {
        sendSimple("{\"roomId\":\""+roomInfo.getMeta().getRoomId()+"\"}",
                PacketType.LOCK_ROOM);
    }

    @FXML
    private void onStartFocus() {
        sendSimple("{\"roomId\":\""+roomInfo.getMeta().getRoomId()+"\"}",
                PacketType.TIMER_FOCUS_START);
    }

    private void sendSimple(String payload, PacketType type) {
        try {
            out.println(mapper.writeValueAsString(new Packet(type, payload)));
        } catch (Exception e) {
            showError("전송 오류: "+e.getMessage());
        }
    }

    /** 톱니바퀴 클릭하면 설정 패널 열기/닫기 */
    @FXML
    private void onToggleSettings() {
        settingsOverlay.setVisible(!settingsOverlay.isVisible());
    }

    /** Apply 누르면 서버에 MODIFY_ROOM 요청 */
    @FXML
    private void onApplySettings() {
        errorLabel.setVisible(false);
        int cur = roomInfo.getCurMembers();
        int max = maxMembersSpinner.getValue();
        if (cur > max) {
            errorLabel.setText("현재 참가자 수보다 큰 값을 입력하세요");
            errorLabel.setVisible(true);
            return;
        }
        String json = String.format(
                "{\"roomId\":\"%s\",\"maxMembers\":%d,\"focusMin\":%d,"
                        + "\"breakMin\":%d,\"loops\":%d,\"allowMidEntry\":%b,"
                        + "\"password\":\"%s\"}",
                roomInfo.getMeta().getRoomId(), max,
                focusSpinner.getValue(), breakSpinner.getValue(),
                loopsSpinner.getValue(), midEntryBox.isSelected(),
                midEntryBox.isSelected()? passwordField.getText() : ""
        );
        sendSimple(json, PacketType.MODIFY_ROOM);
        settingsOverlay.setVisible(false);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static class ParticipantRow {
        private final String name, role;
        public ParticipantRow(String n, String r) { name=n; role=r; }
        public String getName(){return name;}
        public String getRole(){return role;}
    }
}
