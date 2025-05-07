package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.PrintWriter;

public class PrivateRoomJoinController implements PacketListener {

    @FXML private TextField     roomIdField;
    @FXML private PasswordField passwordField;
    @FXML private Button        joinButton;
    @FXML private Button        cancelButton;
    @FXML private Text          errorText;

    private PrintWriter out;
    private MainApp     app;
    private final ObjectMapper mapper = new ObjectMapper();

    public void setDependencies(MainApp app, PrintWriter out) {
        this.app = app;
        this.out = out;
        app.addScreenListener(this); // ACK/ERROR 받기 위함
    }

    @FXML
    public void initialize() {
        joinButton.setOnAction(e -> doJoinPrivateRoom());
        cancelButton.setOnAction(e -> closePopup());
    }

    private void doJoinPrivateRoom() {
        String roomId = roomIdField.getText();
        String password = passwordField.getText();

        if (roomId.isEmpty() || password.isEmpty()) {
            errorText.setText("방 ID와 비밀번호를 모두 입력하세요.");
            return;
        }

        try {
            String payload = String.format(
                    "{\"roomId\":\"%s\",\"password\":\"%s\"}",
                    roomId, password
            );
            Packet pkt = new Packet(PacketType.JOIN_PRIVATE, payload);
            out.println(mapper.writeValueAsString(pkt));
        } catch (Exception ex) {
            showError("요청 전송 오류");
        }
    }

    private void closePopup() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() != PacketType.ACK && pkt.type() != PacketType.ERROR) return;

        Platform.runLater(() -> {
            if (pkt.type() == PacketType.ACK) {
                try {
                    // 1) action 파싱
                    JsonNode root = mapper.readTree(pkt.payloadJson());
                    String action = root.path("action").asText();

                    if ("JOIN_PRIVATE".equals(action)) {
                        // 2) 조인 성공 시 로비(또는 원하는 뷰)로 전환
                        app.forwardTo("/fxml/LobbyView.fxml", pkt);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 3) 팝업은 무조건 닫아줌
                    closePopup();
                }
            }
            else { // ERROR
                try {
                    JsonNode node = mapper.readTree(pkt.payloadJson());
                    errorText.setText(node.get("message").asText());
                } catch (Exception e) {
                    errorText.setText("응답 파싱 실패");
                }
            }
        });
    }


    @Override
    public void onError(Exception e) {
        Platform.runLater(() -> showError("통신 오류: " + e.getMessage()));
    }

    private void showError(String msg) {
        errorText.setText(msg);
    }
}
