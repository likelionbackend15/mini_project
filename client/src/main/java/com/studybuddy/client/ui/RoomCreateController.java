package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.io.PrintWriter;

import com.studybuddy.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomCreateController implements PacketListener {
    @FXML private TextField nameField;
    @FXML private Spinner<Integer> maxMembersSpinner;
    @FXML private Spinner<Integer> focusSpinner, breakSpinner, loopsSpinner;
    @FXML private CheckBox midEntryBox, privateBox;
    @FXML private PasswordField passwordField;
    @FXML private Button createButton, cancelButton;
    @FXML private Text errorText;

    private PrintWriter out;
    private MainApp app;

    private static final Logger log = LoggerFactory.getLogger(RoomCreateController.class);
    @FXML
    public void initialize() {
        // 스피너 초기값 세팅
        maxMembersSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 20, 5));
        focusSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 120, 25));
        breakSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 5));
        loopsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 4));

        // 비공개 방 체크박스 토글에 따라 패스워드 필드 활성/비활성
        privateBox.selectedProperty().addListener((obs, oldV, newV) ->
                passwordField.setDisable(!newV));

        errorText.setVisible(false);

        createButton.setOnAction(e -> doCreate());
        cancelButton.setOnAction(e -> goBack());
    }

    /** MainApp 에서 자동 주입됨 */
    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    /** MainApp 에서 자동 주입됨 */
    public void setApp(MainApp app) {
        this.app = app;
        app.addScreenListener(this);
    }

    /** 방 생성 요청 */
    private void doCreate() {
        errorText.setVisible(false);
        try {
            String payload = String.format(
                    "{\"name\":\"%s\",\"maxMembers\":%d,\"focusMin\":%d,\"breakMin\":%d," +
                            "\"loops\":%d,\"allowMidEntry\":%b,\"password\":\"%s\"}",
                    nameField.getText().trim(),
                    maxMembersSpinner.getValue(),
                    focusSpinner.getValue(),
                    breakSpinner.getValue(),
                    loopsSpinner.getValue(),
                    midEntryBox.isSelected(),
                    privateBox.isSelected() ? passwordField.getText() : ""
            );
            log.debug("🛫 doCreate() called, payload={}", payload);
            Packet pkt = new Packet(PacketType.CREATE_ROOM, payload);
            String jsonPkt = JsonUtil.mapper().writeValueAsString(pkt);
            log.debug("🛫 sending Packet: {}", jsonPkt);
            out.println(jsonPkt);
        } catch (Exception ex) {
            Platform.runLater(() -> {
                errorText.setText("방 생성 오류: " + ex.getMessage());
                errorText.setVisible(true);
            });
        }
    }

    /** 서버로부터 오는 패킷 처리 (ACK / ERROR) */
    @Override
    public void onPacket(Packet pkt) {
        log.debug("⬅️ RoomCreateController.onPacket: type={} payload={}",
                pkt.type(), pkt.payloadJson());
        if (pkt.type() == PacketType.ACK) {
            // ACK 전체를 UI 스레드에서 처리
            Platform.runLater(() -> {
                try {
                    // 1) wrapper 전체 Tree 로 읽고
                    JsonNode root = JsonUtil.mapper().readTree(pkt.payloadJson());
                    String action = root.path("action").asText();

                    if ("CREATE_ROOM".equals(action)) {
                        // 2) info 필드만 꺼내고
                        JsonNode infoNode = root.get("info");
                        // 3) infoNode JSON 문자열을 payload 로 쓰는 새 Packet 생성
                        Packet infoPkt = new Packet(PacketType.ACK, infoNode.toString());
                        // 4) RoomHostView 로 화면 전환 (첫 패킷으로 전달)
                        app.forwardTo("/fxml/RoomHostView.fxml", infoPkt);
                    }
                } catch (Exception ex) {
                    errorText.setText("응답 처리 오류: " + ex.getMessage());
                    errorText.setVisible(true);
                }
            });
        }
        else if (pkt.type() == PacketType.ERROR) {
            Platform.runLater(() -> {
                try {
                    String msg = JsonUtil.mapper()
                            .readTree(pkt.payloadJson())
                            .path("message").asText();
                    errorText.setText(msg);
                    errorText.setVisible(true);
                } catch (Exception e) {
                    errorText.setText("알 수 없는 오류");
                    errorText.setVisible(true);
                }
            });
        }
    }


    @Override
    public void onError(Exception e) {
        Platform.runLater(() -> {
            errorText.setText("네트워크 오류: " + e.getMessage());
            errorText.setVisible(true);
        });
    }

    /** 로비 화면으로 돌아가기 */
    private void goBack() {
        Platform.runLater(() ->
                app.forwardTo("/fxml/LobbyView.fxml", null)
        );
    }
}
