package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.io.PrintWriter;
import java.net.Socket;

public class LoginController implements PacketListener {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Text          errorText;

    private Socket socket;
    private PrintWriter out;
    private MainApp app;                   // MainApp 참조
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        loginButton.setOnAction(e -> doLogin());
    }

    /* MainApp이 의존성 주입 */
    public void setWriter(PrintWriter out) { this.out = out; }
    public void setApp(MainApp app)        { this.app = app; app.addScreenListener(this); }

    /* === 로그인 버튼 === */
    private void doLogin() {
        try {
            String payload = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\"}",
                    usernameField.getText(), passwordField.getText());

            out.println(mapper.writeValueAsString(
                    new Packet(PacketType.LOGIN, payload)));
        } catch (Exception ex) {
            showError("로그인 요청 오류");
        }
    }

    /* === PacketListener 구현 === */
    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() != PacketType.ACK) return;

        try {
            var node = mapper.readTree(pkt.payloadJson());
            if ("LOGIN".equals(node.get("action").asText())) {
                Platform.runLater(() ->
                        app.forwardTo("/fxml/LobbyView.fxml", pkt));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override public void onError(Exception e) { showError(e.getMessage()); }

    private void showError(String msg) { errorText.setText(msg); }
}
