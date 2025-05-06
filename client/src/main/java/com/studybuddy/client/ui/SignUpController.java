package com.studybuddy.client.ui;

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

/**
 * 회원가입 화면 컨트롤러
 *
 * 1) Send Code 버튼  → (추후) 이메일 인증 코드 발송 요청
 * 2) Sign Up 버튼    → SIGNUP 패킷 전송
 * 3) 서버 ACK        → 로그인 화면으로 전환
 * 4) 서버 ERROR      → 오류 메시지 출력
 */
public class SignUpController implements PacketListener {

    /* ---------- FXML 바인딩 ---------- */
    @FXML private TextField     idField;
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
        /* 이메일 인증(추후 구현) */
        sendCodeButton.setOnAction(e -> sendEmailCode());

        /* 회원가입 */
        signUpButton.setOnAction(e -> doSignUp());

        /* 이미 계정 있음 → 로그인 화면으로 */
        loginLink.setOnAction(e ->
                app.forwardTo("/fxml/LoginView.fxml", null));

        errorLabel.setVisible(false);
    }

    /* ---------- MainApp 이 주입 ---------- */
    public void setWriter(PrintWriter out) { this.out = out; }
    public void setApp(MainApp app)        { this.app = app; app.addScreenListener(this); }

    /* ======================================================
       1) Send Code 버튼 : 이메일 코드 발송 (stub)
    ====================================================== */
    private void sendEmailCode() {
        // ★ 서버에서 이메일 인증 로직을 아직 구현하지 않았다면, 로컬에서 안내만 표시
        showError("이메일 인증 기능은 추후 제공됩니다.");
    }

    /* ======================================================
       2) Sign Up 버튼 : SIGNUP 패킷 전송
    ====================================================== */
    private void doSignUp() {
        /* 2-1. 클라이언트 측 유효성 검사 */
        String username = idField.getText().trim();
        String email    = emailField.getText().trim();
        String pw1      = passwordField.getText();
        String pw2      = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || pw1.isEmpty() || pw2.isEmpty()) {
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

        /* 2-2. 서버로 SIGNUP 패킷 전송 */
        try {
            String payload = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\",\"email\":\"%s\"}",
                    username, pw1, email);

            out.println(mapper.writeValueAsString(
                    new Packet(PacketType.SIGNUP, payload)));

        } catch (Exception e) {
            showError("요청 오류: " + e.getMessage());
        }
    }

    /* ======================================================
       3) PacketListener 구현
    ====================================================== */
    @Override
    public void onPacket(Packet pkt) {
        switch (pkt.type()) {
            case ACK -> handleAck(pkt);
            case ERROR -> showError(extractError(pkt));
            default -> { /* 무시 */ }
        }
    }

    private void handleAck(Packet pkt) {
        /* 가입 성공 → 로그인 화면으로 전환하면서 성공 메시지 전달 */
        Platform.runLater(() ->
                app.forwardTo("/fxml/LoginView.fxml", pkt));
    }

    /* 서버 ERROR 페이로드에서 message 필드 추출 */
    private String extractError(Packet pkt) {
        try {
            return mapper.readTree(pkt.payloadJson())
                    .path("message").asText("Unknown error");
        } catch (Exception e) { return "Unknown error"; }
    }

    @Override
    public void onError(Exception e) { showError(e.getMessage()); }

    /* ======================================================
       4) 공통 헬퍼
    ====================================================== */
    private void showError(String msg) {
        Platform.runLater(() -> {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        });
    }
}
