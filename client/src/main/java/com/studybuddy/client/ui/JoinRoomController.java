package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 방 입장 화면 컨트롤러
 * • JOIN_ROOM 응답으로 ROOM_INIT 수신 시 PomodoroView 로 전환
 */
public class JoinRoomController implements PacketListener {
    @FXML private TextField roomIdField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox privateBox;
    @FXML private Button joinButton;
    @FXML private Button cancelButton;
    @FXML private Text errorText;

    private PrintWriter out;
    private MainApp app;
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        privateBox.selectedProperty().addListener((o, oldV, newV) ->
                passwordField.setDisable(!newV)
        );
        joinButton.setOnAction(e -> doJoin());
        cancelButton.setOnAction(e -> goBack());
    }

    /** MainApp 과 PrintWriter 주입 */
    public void setDependencies(MainApp app, PrintWriter out) {
        this.app = app;
        this.out = out;
        app.addScreenListener(this);
    }

    private void doJoin() {
        errorText.setText("");
        try {
            String payload = String.format(
                    "{\"roomId\":\"%s\",\"password\":\"%s\"}",
                    roomIdField.getText().trim(),
                    privateBox.isSelected() ? passwordField.getText().trim() : ""
            );
            Packet pkt = new Packet(PacketType.JOIN_ROOM, payload);
            out.println(mapper.writeValueAsString(pkt));
        } catch (Exception ex) {
            errorText.setText("입장 요청 전송 실패");
        }
    }

    private void goBack() {
        // 로비 화면으로 복귀
        Platform.runLater(() -> app.forwardTo("/fxml/RoomListView.fxml", null));
    }

    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() == PacketType.ROOM_INIT) {
            Platform.runLater(() -> {
                try {
                    // 1) FXML 로드
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/fxml/PomodoroView.fxml")
                    );
                    Parent root = loader.load();

                    // 2) 컨트롤러 가져오기
                    PomodoroController ctrl = loader.getController();

                    // 3) writer, listener, 초기 패킷 전달
                    ctrl.setWriter(out);
                    app.addScreenListener(ctrl);
                    ctrl.onPacket(pkt);   // → initRoom + loadChatHistory 호출

                    // 4) 화면 전환
                    Stage stage = (Stage) joinButton.getScene().getWindow();
                    stage.getScene().setRoot(root);

                } catch (IOException e) {
                    errorText.setText("화면 전환 실패: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else if (pkt.type() == PacketType.ERROR) {
            try {
                JsonNode root = mapper.readTree(pkt.payloadJson());
                errorText.setText(root.path("message").asText());
            } catch (Exception e) {
                errorText.setText("입장 실패");
            }
        }
    }

    @Override
    public void onError(Exception e) {
        Platform.runLater(() -> errorText.setText("통신 오류: " + e.getMessage()));
    }
}
