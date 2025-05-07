package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import com.studybuddy.client.model.UserSession;
import com.studybuddy.common.domain.User;

import java.io.PrintWriter;
import java.net.Socket;

public class LoginController implements PacketListener {

    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label errorLabel;
    @FXML private Hyperlink     signUpLink;
    @FXML private Hyperlink forgotPasswordLink;

    private Socket socket;
    private PrintWriter out;
    private MainApp app;                   // MainApp 참조
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        // 로그인 버튼
        loginButton.setOnAction(e -> doLogin());

        // ★ 회원가입 링크 클릭 시 SignUp 뷰로 이동
        signUpLink.setOnAction(e ->
                app.forwardTo("/fxml/SignUpView.fxml", null)
        );

        // Forgot password 으로 이동
        forgotPasswordLink.setOnAction(e ->
                app.forwardTo("/fxml/ResetPasswordView.fxml", null)
        );
    }

    /* MainApp이 의존성 주입 */
    public void setWriter(PrintWriter out) { this.out = out; }
    public void setApp(MainApp app)        { this.app = app; app.addScreenListener(this); }

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
            var node = mapper.readTree(pkt.payloadJson());
            if ("LOGIN".equals(node.get("action").asText())) {
                // 1) 서버가 보낸 user 객체 파싱
                        User u = mapper.treeToValue(node.get("user"), User.class);
                // 2) 전역 세션에 저장
                        UserSession.getInstance().setUser(u);
                // 3) 메인 스레드에서 로비로 화면 전환
                        Platform.runLater(() ->
                                app.forwardTo("/fxml/RoomCreateView.fxml", null));
                           }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override public void onError(Exception e) { showError(e.getMessage()); }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
