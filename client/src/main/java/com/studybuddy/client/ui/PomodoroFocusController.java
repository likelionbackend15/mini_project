package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.studybuddy.common.util.JsonUtil;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.model.TimerModel;
import com.studybuddy.client.model.UserSession;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 뽀모도로 집중 화면 컨트롤러
 * • TIMER_FOCUS_START / TIMER_TICK 으로 타이머 갱신
 * • CHAT 패킷으로 실시간 채팅
 */
public class PomodoroFocusController implements PacketListener {
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label timeLabel;
    @FXML private Label cycleLabel;
    @FXML private ListView<String> participantsList;
    @FXML private ListView<String> chatList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    private MainApp app;
    private PrintWriter out;
    private final TimerModel timerModel = new TimerModel();
    private final ObservableList<String> participants = FXCollections.observableArrayList();
    private final ObservableList<String> chats        = FXCollections.observableArrayList();

    // 화면 초기화에 필요한 값
    private String roomId;
    private int totalFocusSec;
    private int totalLoops;
    private int currentLoop;

    private static final Logger log = LoggerFactory.getLogger(PomodoroFocusController.class);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        participantsList.setItems(participants);
        chatList.setItems(chats);

        // 채팅 보내기
        sendButton.setOnAction(e -> sendChat());
        messageField.setOnAction(e -> sendChat());
    }

    /** MainApp 에서 주입됩니다. */
    public void setApp(MainApp app) {
        this.app = app;
        app.addScreenListener(this);
    }

    /** MainApp 에서 주입됩니다. */
    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    /** RoomHostController에서 화면 전환 시 초기 패킷을 전달 */
    public void setInitData(Packet initPkt) {
        try {
            JsonNode root = JsonUtil.mapper().readTree(initPkt.payloadJson());
            // ex) payload: { roomId, focusSec, loopIdx, totalLoops, members: [...] }
            roomId        = root.path("roomId").asText();
            totalFocusSec = root.path("focusSec").asInt();
            currentLoop   = root.path("loopIdx").asInt();
            totalLoops    = root.path("totalLoops").asInt();
            // 참가자 초기화
            participants.clear();
            for (JsonNode u : root.withArray("members")) {
                String who = u.path("name").asText();
                String role= u.path("role").asText();
                participants.add(who + "  (" + role + ")");
            }
            // 화면 갱신: 타이머·사이클
            updateTimer(root.path("remainingSec").asInt());
        } catch (Exception ex) {
            log.error("초기 데이터 처리 실패", ex);
        }
    }

    @Override
    public void onPacket(Packet pkt) {
        try {
            JsonNode root = JsonUtil.mapper().readTree(pkt.payloadJson());
            switch (pkt.type()) {
                case TIMER_FOCUS_START -> {
                    Platform.runLater(() -> {
                        currentLoop = root.path("loopIdx").asInt();
                        updateTimer(root.path("remainingSec").asInt());
                    });
                }
                case TIMER_TICK -> {
                    Platform.runLater(() ->
                            updateTimer(root.path("remainingSec").asInt())
                    );
                }
                case CHAT -> {
                    Platform.runLater(() ->
                            addChat(root.path("sender").asText(),
                                    root.path("text").asText())
                    );
                }
                default -> { /* 무시 */ }
            }
        } catch (Exception ex) {
            log.error("패킷 처리 오류", ex);
        }
    }

    private void updateTimer(int remainingSec) {
        // 모델 업데이트
        timerModel.setRemainingSec(remainingSec);
        // UI 갱신
        timeLabel.setText(timerModel.getFormattedTime());
        progressIndicator.setProgress(
                (double) remainingSec / totalFocusSec
        );
        cycleLabel.setText(
                String.format("Cycle %d of %d · %d min focus",
                        currentLoop, totalLoops, totalFocusSec / 60)
        );
    }

    private void sendChat() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        try {
            // sender 필드를 getId() 로 대체
            String sender = UserSession.getInstance().getCurrentUser().getId();
            String payload = String.format(
                    "{\"roomId\":\"%s\",\"sender\":\"%s\",\"text\":\"%s\"}",
                    roomId,
                    sender,
                    text
            );
            Packet chatPkt = new Packet(PacketType.CHAT, payload);
            out.println(JsonUtil.mapper().writeValueAsString(chatPkt));
            messageField.clear();
        } catch (Exception ex) {
            log.error("CHAT 전송 실패", ex);
        }
    }


    private void addChat(String sender, String text) {
        String time = LocalTime.now().format(TIME_FMT);
        chats.add(String.format("%s  %s: %s", time, sender, text));
        // 스크롤을 마지막으로
        chatList.scrollTo(chats.size() - 1);
    }

    @Override
    public void onError(Exception e) {
        log.error("네트워크 오류", e);
    }
}
