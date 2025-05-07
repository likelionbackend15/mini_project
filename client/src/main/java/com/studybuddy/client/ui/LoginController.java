package com.studybuddy.client.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import com.studybuddy.common.domain.User;

import java.io.PrintWriter;
import java.net.Socket;

public class LoginController implements PacketListener {

    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel; // ← Text → Label 로 변경
    @FXML private Hyperlink     signUpLink;
    @FXML private Hyperlink     forgotPasswordLink;

    private Socket socket;
    private PrintWriter out;

    private MainApp app;                   // MainApp 참조
    private static final ObjectMapper mapper = JsonUtil.mapper();


    @FXML
    public void initialize() {
        loginButton.setOnAction(e -> doLogin());

        signUpLink.setOnAction(e ->
                app.forwardTo("/fxml/SignUpView.fxml", null)
        );

        forgotPasswordLink.setOnAction(e ->
                app.forwardTo("/fxml/ResetPasswordView.fxml", null)
        );
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    public void setApp(MainApp app) {
        this.app = app;
        app.addScreenListener(this);
    }


    /* 로그인 패킷 전송 ----------------------------------------------------- */

    private void doLogin() {
        try {
            String payload = String.format(
                    "{\"id\":\"%s\",\"password\":\"%s\"}",
                    idField.getText(), passwordField.getText());

            String json = JsonUtil.mapper().writeValueAsString(new Packet(PacketType.LOGIN, payload));
            out.println(json);
        } catch (Exception ex) {
            showError("로그인 요청 오류: " + ex.getMessage());
        }
    }


    /* 서버 응답 처리 -------------------------------------------------------- */

    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() != PacketType.ACK) return;

        try {

            JsonNode root = mapper.readTree(pkt.payloadJson());
            String action = root.path("action").asText();

            if ("LOGIN".equals(action)) {
                // 서버가 보낸 user 객체 파싱
                User u = mapper.treeToValue(root.get("user"), User.class);

                // 전역 세션에 저장
                UserSession.getInstance().setUser(u);

                // 메인 스레드에서 로비로 화면 전환
                Platform.runLater(() ->
                        app.forwardTo("/fxml/RoomCreateView.fxml", null)
                );
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onError(Exception e) {
        showError(e.getMessage());
    }

    private void showError(String msg) {
        errorLabel.setText(msg);

        errorLabel.setVisible(true); // 숨겨져 있다면 표시

        errorLabel.setVisible(true);

    }
}
