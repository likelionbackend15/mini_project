package com.studybuddy.client.ui;
import com.fasterxml.jackson.core.JsonProcessingException;

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



import java.io.PrintWriter;
import java.net.Socket;
public class DeleteAccountController implements PacketListener{

    @FXML private PasswordField passwordField;
    @FXML private Button        cancelButton;
    @FXML private Button        deleteButton;
    @FXML private Label         errorLabel;

    private Socket socket;
    private PrintWriter out;

    private MainApp app;                   // MainApp 참조
    private static final ObjectMapper mapper = JsonUtil.mapper();

    /** MainApp.forwardTo()에서 자동 호출됩니다 */
    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    /** MainApp.forwardTo()에서 자동 호출됩니다 */
    public void setApp(MainApp app) {
        this.app = app;
    }


    @FXML
    public void initialize() {
        deleteButton.setOnAction(e -> deleteAccount());

        cancelButton.setOnAction(e -> cancelDelete());

        errorLabel.setVisible(false);
    }

    private void cancelDelete() {
        app.forwardTo("/fxml/LobbyView.fxml", null);
    }

    private void deleteAccount() {

            try {
                String payload = String.format(
                        "{\"id\":\"%s\",\"password\":\"%s\"}",
                        UserSession.getInstance().getCurrentUser().getId(),passwordField.getText());

                String json = JsonUtil.mapper().writeValueAsString(new Packet(PacketType.DELETE_ACCOUNT, payload));
                out.println(json);
            } catch (Exception ex) {
                showError("계정 삭제 오류 : " + ex.getMessage());
            }


    }


    @Override
    public void onPacket(Packet pkt) {
        if (pkt.type() != PacketType.ACK) return;

        try {
            JsonNode root = mapper.readTree(pkt.payloadJson());
            String action = root.path("action").asText();

            if ("DELETE_ACCOUNT".equals(action)) {

                // 로그아웃
                UserSession.getInstance().clear();
                // 2) 화면 전환: LobbyVIew
                Platform.runLater(() ->
                        app.forwardTo("/fxml/LoginView.fxml", null)
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
        errorLabel.setVisible(true);
        errorLabel.setText(msg);



    }
}
