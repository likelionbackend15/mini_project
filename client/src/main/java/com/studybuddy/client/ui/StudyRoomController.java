package com.studybuddy.client.ui;

import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.io.PrintWriter;

public class StudyRoomController {
    @FXML private Text         roomNameText;
    @FXML private Text         timerText;
    @FXML private Button       startFocusButton, startBreakButton, leaveButton;
    @FXML private ListView<String> chatList;
    @FXML private TextArea     chatInput;
    @FXML private Button       sendChatButton;

    private PrintWriter out;

    @FXML
    public void initialize() {
        startFocusButton.setOnAction(e -> sendTimer(PacketType.TIMER_FOCUS_START));
        startBreakButton.setOnAction(e -> sendTimer(PacketType.TIMER_BREAK_START));
        sendChatButton.setOnAction(e -> sendChat());
        leaveButton.setOnAction(e -> leaveRoom());
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    private void sendTimer(PacketType type) {
        out.println(createJson(type, ""));  // helper: Packet→JSON
    }

    private void sendChat() {
        var text = chatInput.getText();
        out.println(createJson(PacketType.CHAT, "{\"text\":\""+text+"\"}"));
        chatInput.clear();
    }

    private void leaveRoom() {
        out.println(createJson(PacketType.BACK_TO_LOBBY, ""));
    }

    private String createJson(PacketType type, String body) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(new Packet(type, body));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
