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
import java.io.PrintWriter;
import java.util.regex.Pattern;

public class SignUpController implements PacketListener {

    /* ---------- FXML 바인딩 ---------- */
    @FXML private TextField     idField;
    @FXML private TextField     usernameField;   // 닉네임
    @FXML private TextField     emailField;
    @FXML private TextField     codeField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button        sendCodeButton;
    @FXML private Button        signUpButton;
    @FXML private Label         errorLabel;
    @FXML private Hyperlink     loginLink;

    /* ---------- 의존성 ---------- */
    private PrintWriter out;        // 서버 송신용
    private MainApp     app;        // 화면 전환용
    private final ObjectMapper mapper = new ObjectMapper();

    /* ---------- 초기화 ---------- */
    @FXML
    public void initialize() {
        // 이메일 인증 코드 요청
        sendCodeButton.setOnAction(e -> sendEmailCode());

        // 회원가입
        signUpButton.setOnAction(e -> doSignUp());

        // 로그인 화면으로 이동
        loginLink.setOnAction(e ->
                app.forwardTo("/fxml/LoginView.fxml", null)
        );

        errorLabel.setVisible(false);
    }

    /* ---------- MainApp 이 주입 ---------- */
    public void setWriter(PrintWriter out) { this.out = out; }
    public void setApp(MainApp app)        { this.app = app; app.addScreenListener(this); }

    // --- 1) Send Code 버튼 핸들러 ---
    private void sendEmailCode() {
        String email = emailField.getText().trim();
        if (!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$", email)) {
            showError("올바른 이메일을 입력하세요.");
            return;
        }
        try {
            // {"email":"..."}
            String body = String.format("{\"email\":\"%s\"}", email);
            out.println(mapper.writeValueAsString(
                    new Packet(PacketType.SEND_CODE, body)));
        } catch (Exception ex) {
            showError("코드 요청 실패: " + ex.getMessage());
        }
    }

    // --- 2) Sign Up 버튼 핸들러 ---
    private void doSignUp() {
        String id       = idField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String code     = codeField.getText().trim();
        String pw1      = passwordField.getText();
        String pw2      = confirmPasswordField.getText();

        if (id.isEmpty() || username.isEmpty()
                || email.isEmpty() || code.isEmpty()
                || pw1.isEmpty() || pw2.isEmpty()) {
            showError("모든 필드를 입력하세요.");
            return;
        }
        if (!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$", email)) {
            showError("올바른 이메일 형식이 아닙니다.");
            return;
        }
        if (!pw1.equals(pw2)) {
            showError("비밀번호가 일치하지 않습니다.");
            return;
        }

        try {
            // id, username, password, email, code 모두 포함
            String body = String.format(
                    "{\"id\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"," +
                            "\"email\":\"%s\",\"code\":\"%s\"}",
                    id, username, pw1, email, code
            );
            out.println(mapper.writeValueAsString(
                    new Packet(PacketType.SIGNUP, body)));
        } catch (Exception ex) {
            showError("회원가입 요청 실패: " + ex.getMessage());
        }
    }

    // --- 3) 서버 응답 처리 ---
    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() == PacketType.ACK) {
            try {
                JsonNode root = mapper.readTree(pkt.payloadJson());
                String action = root.path("action").asText();

                if ("SEND_CODE".equals(action)) {
                    // 인증 코드 발송 성공
                    Platform.runLater(() ->
                            showInfo("인증 코드가 이메일로 발송되었습니다.")
                    );
                }
                else if ("SIGNUP".equals(action)) {
                    // 회원가입 성공 → 로그인 화면으로
                    Platform.runLater(() ->
                            app.forwardTo("/fxml/LoginView.fxml", pkt)
                    );
                }
            } catch (Exception e) {
                showError("응답 처리 오류: " + e.getMessage());
            }
        }
        else if (pkt.type() == PacketType.ERROR) {
            // 서버가 보낸 {"message":"..."} 중 message 필드만 뽑아서
            String msg;
            try {
                msg = mapper.readTree(pkt.payloadJson())
                        .path("message").asText();
            } catch (Exception e) {
                msg = "알 수 없는 오류";
            }
            showError(msg);
        }
    }

    @Override public void onError(Exception e) { showError(e.getMessage()); }

    // --- 헬퍼 ---
    private void showError(String msg) {
        Platform.runLater(() -> {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        });
    }
    private void showInfo(String msg) {
        Platform.runLater(() ->
                new Alert(Alert.AlertType.INFORMATION, msg).showAndWait()
        );
    }
}
