package com.studybuddy.client.ui;

import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.io.PrintWriter;

public class RoomCreateController {
    @FXML private TextField nameField;
    @FXML private Spinner<Integer> maxMembersSpinner;
    @FXML private Spinner<Integer> focusSpinner, breakSpinner, loopsSpinner;
    @FXML private CheckBox midEntryBox, privateBox;
    @FXML private PasswordField passwordField;
    @FXML private Button createButton, cancelButton;
    @FXML private Text errorText;

    private PrintWriter out;

    @FXML
    public void initialize() {
        // 초기값 세팅
        maxMembersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 20, 5));
        focusSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 120, 25));
        breakSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 5));
        loopsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 4));
        privateBox.selectedProperty().addListener((o, oldV, newV) -> passwordField.setDisable(!newV));

        createButton.setOnAction(e -> doCreate());
        cancelButton.setOnAction(e -> goBack());
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    private void doCreate() {
        try {
            // JSON payload 생성
            String payload = String.format(
                    "{\"name\":\"%s\",\"maxMembers\":%d,\"focusMin\":%d,\"breakMin\":%d,\"loops\":%d,\"allowMidEntry\":%b,\"password\":\"%s\"}",
                    nameField.getText(),
                    maxMembersSpinner.getValue(),
                    focusSpinner.getValue(),
                    breakSpinner.getValue(),
                    loopsSpinner.getValue(),
                    midEntryBox.isSelected(),
                    privateBox.isSelected() ? passwordField.getText() : ""
            );
            Packet pkt = new Packet(PacketType.CREATE_ROOM, payload);
            out.println(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(pkt));
            // TODO: 응답 수신 → StudyRoomController 로 전환
        } catch (Exception ex) {
            errorText.setText("방 생성 오류");
        }
    }

    private void goBack() {
        // TODO: LobbyController 로 장면 전환
    }
}
