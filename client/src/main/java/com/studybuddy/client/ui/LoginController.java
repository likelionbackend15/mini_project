package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.model.UserSession;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.net.Socket;

public class LoginController {
    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button       loginButton;
    @FXML private Text         errorText;

    private Socket socket;
    private PrintWriter out;
    private final ObjectMapper mapper = new ObjectMapper();

    /** FXML 로딩 후 호출 */
    @FXML
    public void initialize() {
        loginButton.setOnAction(e -> doLogin());
    }

    public void setConnection(Socket socket, PrintWriter out) {
        this.socket = socket;
        this.out    = out;
    }

    private void doLogin() {
        try {
            var payload = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\"}",
                    usernameField.getText(),
                    passwordField.getText()
            );
            Packet pkt = new Packet(PacketType.LOGIN, payload);
            out.println(mapper.writeValueAsString(pkt));
            // TODO: 서버 응답 리스닝 → 성공 시 UserSession.setUser(...) + 화면 전환
        } catch (Exception ex) {
            errorText.setText("로그인 요청 중 오류 발생");
            ex.printStackTrace();
        }
    }
}
