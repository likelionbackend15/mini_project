package com.studybuddy.client.ui;

import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.PrintWriter;

public class JoinRoomController {
    @FXML private TextField roomIdField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox privateBox;
    @FXML private Button     joinButton, cancelButton;
    @FXML private Text       errorText;

    private PrintWriter out;

    @FXML
    public void initialize() {
        privateBox.selectedProperty().addListener((o, oldV, newV) ->
                passwordField.setDisable(!newV)
        );
        joinButton.setOnAction(e -> doJoin());
        cancelButton.setOnAction(e -> goBack());
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    private void doJoin() {
        try {
            String payload = String.format(
                    "{\"roomId\":\"%s\",\"password\":\"%s\"}",
                    roomIdField.getText(),
                    privateBox.isSelected() ? passwordField.getText() : ""
            );
            Packet pkt = new Packet(PacketType.JOIN_ROOM, payload);
            out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(pkt));
            // TODO: 응답 수신 → StudyRoomController 로 전환
        } catch (Exception ex) {
            errorText.setText("입장 실패");
        }
    }

    private void goBack() {
        // TODO: LobbyController 로 전환
    }
}
