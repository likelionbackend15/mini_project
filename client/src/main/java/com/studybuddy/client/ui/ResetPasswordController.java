package com.studybuddy.client.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
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
 * 비밀번호 재설정 화면 컨트롤러
 *
 * 프로토콜 가정:
 *  - SEND_CODE      : {"email": "..."}
 *  - RESET_PASSWORD : {"email": "...","code":"...","newPw":"..."}
 *  서버는 둘 다 ACK / ERROR 로 응답
 */
public class ResetPasswordController implements PacketListener {

    /* ---------- FXML ---------- */
    @FXML private TextField     emailField;
    @FXML private TextField     codeField;
    @FXML private PasswordField newPwField;
    @FXML private PasswordField confirmPwField;
    @FXML private Button        sendCodeButton;
    @FXML private Button        resetButton;
    @FXML private Hyperlink     backLoginLink;
    @FXML private Label         errorLabel;

    /* ---------- 의존성 ---------- */
    private PrintWriter out;
    private MainApp     app;
    private final ObjectMapper mapper = new ObjectMapper();

    /* ---------- 초기화 ---------- */
    @FXML
    public void initialize() {
        sendCodeButton.setOnAction(e -> sendCode());
        resetButton.setOnAction(e -> resetPassword());
        backLoginLink.setOnAction(e ->
                app.forwardTo("/fxml/LoginView.fxml", null));
        errorLabel.setVisible(false);
    }

    /* ---------- MainApp 주입 ---------- */
    public void setWriter(PrintWriter out) { this.out = out; }
    public void setApp(MainApp app)        { this.app = app; app.addScreenListener(this); }

    /* =========================================================
       1) 이메일 코드 전송
    ========================================================= */
    private void sendCode() {
        String email = emailField.getText().trim();
        if (!isEmail(email)) { showError("올바른 이메일을 입력하세요."); return; }

        try {
            String body = "{\"email\":\"" + email + "\"}";
            out.println(mapper.writeValueAsString(
                    new Packet(PacketType.SEND_CODE, body)));
        } catch (Exception e) { showError("요청 실패"); }
    }

    /* =========================================================
       2) 비밀번호 재설정
    ========================================================= */
    private void resetPassword() {
        String email = emailField.getText().trim();
        String code  = codeField.getText().trim();
        String pw1   = newPwField.getText();
        String pw2   = confirmPwField.getText();

        if (!isEmail(email) || code.isEmpty() || pw1.isEmpty()) {
            showError("모든 필드를 입력하세요."); return;
        }
        if (!pw1.equals(pw2)) { showError("비밀번호가 일치하지 않습니다."); return; }

        try {
            String body = String.format(
                    "{\"email\":\"%s\",\"code\":\"%s\",\"newPw\":\"%s\"}",
                    email, code, pw1);
            out.println(mapper.writeValueAsString(
                    new Packet(PacketType.RESET_PASSWORD, body)));
        } catch (Exception e) { showError("요청 실패"); }
    }

    /* =========================================================
       3) 서버 응답 처리
    ========================================================= */
    @Override
    public void onPacket(Packet pkt) {
        try {
            switch (pkt.type()) {
                case ACK   -> handleAck(pkt);
                case ERROR -> showError(extractError(pkt));
                default    -> {}
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("패킷 처리 중 오류 발생: " + e.getMessage());
        }
    }


    private void handleAck(Packet pkt) throws JsonProcessingException {
        String action = mapper.readTree(pkt.payloadJson())
                .path("action").asText();

        if ("SEND_CODE".equals(action)) {
            showInfo("인증 코드가 이메일로 발송되었습니다.", null);
        } else if ("RESET_OK".equals(action)) {
            showInfo("비밀번호가 변경되었습니다. 다시 로그인하세요.", () -> {
                if (app != null) {
                    app.forwardTo("/fxml/LoginView.fxml", null);
                }
            });
        }
    }

    @Override public void onError(Exception e) { showError(e.getMessage()); }

    /* =========================================================
       4) 유틸
    ========================================================= */
    private boolean isEmail(String s) {
        return Pattern.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$", s);
    }

    private String extractError(Packet pkt) {
        try { return mapper.readTree(pkt.payloadJson())
                .path("message").asText("Unknown error");
        } catch (Exception e) { return "Unknown error"; }
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        });
    }

    private void showInfo(String msg, Runnable afterClose) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
            alert.setHeaderText(null);
            alert.setTitle("알림");
            alert.showAndWait(); // 사용자가 확인 누를 때까지 기다림
            if (afterClose != null) {
                afterClose.run();
            }
        });
    }
}
