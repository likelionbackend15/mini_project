package com.studybuddy.client.ui;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.studybuddy.client.model.UserSession;



import java.io.PrintWriter;
import java.net.Socket;
public class DeleteAccountController implements PacketListener{

    @FXML private PasswordField passwordField;
    @FXML private Button        cancelButton;
    @FXML private Button        deleteButton;
    @FXML private Label         errorLabel;

    private Socket socket;
    private PrintWriter out;

    private MainApp app;                   // MainApp 참조
    private static final ObjectMapper mapper = JsonUtil.mapper();

    /** MainApp.forwardTo()에서 자동 호출됩니다 */
    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    /** MainApp.forwardTo()에서 자동 호출됩니다 */
    public void setApp(MainApp app) {
        this.app = app;
    }


    @FXML
    public void initialize() {
        deleteButton.setOnAction(e -> deleteAccount());

        cancelButton.setOnAction(e -> cancelDelete());

        errorLabel.setVisible(false);
    }

    private void cancelDelete() {
        app.forwardTo("/fxml/LobbyView.fxml", null);
    }

    private void deleteAccount() {
        String password = passwordField.getText();

        if (password == null || password.isEmpty()) {
            showError("비밀번호를 입력해주세요.");
            return;
        }

        try {
            String id = UserSession.getInstance().getCurrentUser().getId();
            String payload = String.format("{\"id\":\"%s\", \"password\":\"%s\"}", id, password);

            // 삭제 요청 패킷 생성
            Packet packet = new Packet(PacketType.DELETE_ACCOUNT, payload);

            // 디버깅 로그
            System.out.println("전송 패킷: " + mapper.writeValueAsString(packet));

            // 서버에 전송
            out.println(mapper.writeValueAsString(packet));

        } catch (Exception e) {
            showError("삭제 요청 중 오류 발생: " + e.getMessage());
        }
    }



    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() != PacketType.ACK) return;

        try {
            JsonNode root = mapper.readTree(pkt.payloadJson());
            String action = root.path("action").asText();

            if ("DELETE_ACCOUNT".equals(action)) {
                // 1. 세션 제거
                UserSession.getInstance().clear();

                // 2. 로그인 화면으로 이동 (예시)
                Platform.runLater(() -> app.forwardTo("/fxml/LoginView.fxml", null));
            }
        } catch (Exception e) {
            showError("서버 응답 처리 중 오류 발생: " + e.getMessage());
        }
    }


    @Override
    public void onError(Exception e) {
        showError(e.getMessage());
    }

    private void showError(String msg) {
        errorLabel.setVisible(true);
        errorLabel.setText(msg);



    }
}
