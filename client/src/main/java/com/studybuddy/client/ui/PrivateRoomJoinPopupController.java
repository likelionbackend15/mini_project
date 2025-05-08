package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.PrintWriter;

public class PrivateRoomJoinPopupController {

    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private String roomId;
    private PrintWriter out;
    private ObjectMapper mapper = new ObjectMapper();

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    @FXML
    private void handleJoin() {
        errorLabel.setVisible(false);
        String pw = passwordField.getText();
        if (pw.isEmpty()) {
            errorLabel.setText("비밀번호를 입력하세요.");
            errorLabel.setVisible(true);
            return;
        }

        try {
            String payload = String.format("{\"roomId\":\"%s\", \"password\":\"%s\"}", roomId, pw);
            out.println(mapper.writeValueAsString(new Packet(PacketType.JOIN_PRIVATE, payload)));
            ((Stage) passwordField.getScene().getWindow()).close();
        } catch (Exception e) {
            errorLabel.setText("전송 실패: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) passwordField.getScene().getWindow()).close();
    }
}
