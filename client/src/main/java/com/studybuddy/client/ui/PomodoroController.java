package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.studybuddy.common.util.JsonUtil;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 통합 뽀모도로 컨트롤러
 * • ROOM_INIT 으로 초기 메타+히스토리 로드
 * • TIMER_FOCUS_START / TIMER_BREAK_START 으로 단계 전환
 * • TIMER_TICK 으로 남은 시간 갱신
 * • CHAT 으로 실시간 채팅
 */
public class PomodoroController implements PacketListener {
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label phaseLabel;         // Focus / Break 표시
    @FXML private Label timeLabel;
    @FXML private Label cycleLabel;
    @FXML private ListView<String> participantsList;
    @FXML private ListView<String> chatList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    private PrintWriter out;
    private final TimerModel timerModel = new TimerModel();
    private final ObservableList<String> participants = FXCollections.observableArrayList();
    private final ObservableList<String> chats        = FXCollections.observableArrayList();
    private final Map<String,String> userMap = new HashMap<>();

    private String roomId;
    private int totalFocusSec;
    private int totalBreakSec;
    private int totalLoops;
    private int currentLoop;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final Logger log = LoggerFactory.getLogger(PomodoroController.class);

    @FXML
    public void initialize() {
        participantsList.setItems(participants);
        chatList.setItems(chats);

        // 기본 사용자
        String myId = UserSession.getInstance().getCurrentUser().getId();
        String me   = UserSession.getInstance().getCurrentUser().getUsername();
        userMap.clear(); participants.clear();
        userMap.put(myId, me);
        participants.add(String.format("%s(%s) - Host", me, myId));

        sendButton.setDefaultButton(true);
        sendButton.setOnAction(e -> sendChat());
        messageField.setOnAction(null);

        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 48px;");
        cycleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
        phaseLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
    }

    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void onPacket(Packet pkt) {
        try {
            log.debug("[🍅 Pomodoro] onPacket type={} payload={}", pkt.type(), pkt.payloadJson());
            JsonNode root = JsonUtil.mapper().readTree(pkt.payloadJson());
            switch (pkt.type()) {
                case ROOM_INIT -> Platform.runLater(() -> {
                    // 1) 페이로드 JSON 전체를 로그에 출력
                    log.debug("∘∘∘ ROOM_INIT payload:\n{}", root.toPrettyString());

                    // ① ROOM_INIT 에서도 메타 정보(분이 아니라 초 단위 혹은 서버가 주는 키 확인) 읽기
                    roomId        = root.path("roomId").asText();
                    totalFocusSec = root.path("focusSec").asInt();
                    totalBreakSec = root.path("breakSec").asInt();
                    totalLoops    = root.path("totalLoops").asInt(); // 서버가 "totalLoops" 로 보내주는지 확인!
                    currentLoop   = root.path("loopIdx").asInt();    // 서버가 보내주지 않으면 1로 초기화
                    initRoom(root);
                    loadChatHistory(root.withArray("chatHistory"));
                    });
                case TIMER_FOCUS_START -> Platform.runLater(() -> {
                    // ② 포커스 시작 페이즈 + 메타 갱신
                    timerModel.setPhase("FOCUS");
                    totalFocusSec = root.path("focusSec").asInt();
                    totalBreakSec = root.path("breakSec").asInt();
                    totalLoops    = root.path("totalLoops").asInt();
                    currentLoop   = root.path("loopIdx").asInt();
                    updatePhaseUI();
                               });
                case TIMER_BREAK_START -> Platform.runLater(() -> {
                    // ③ 브레이크 시작 페이즈 + 메타 갱신
                    timerModel.setPhase("BREAK");
                    totalFocusSec = root.path("focusSec").asInt();
                    totalBreakSec = root.path("breakSec").asInt();
                    totalLoops    = root.path("totalLoops").asInt();
                    currentLoop   = root.path("loopIdx").asInt();
                    updatePhaseUI();
                               });
                case TIMER_TICK -> Platform.runLater(() -> {
                    // ④ 틱마다 서버가 주는 phase(FOCUS/BREAK) 반영
                    String phaseVal = root.path("phase").asText();
                    timerModel.setPhase(phaseVal);
                    phaseLabel.setText(phaseVal.equals("FOCUS") ? "Focus" : "Break");

                    // 남은 시간 업데이트
                    int remaining = root.path("remainingSec").asInt();
                    timerModel.setRemainingSec(remaining);
                    updateTimer(remaining);
                               });
                case CHAT -> Platform.runLater(() -> {
                    addChat(root.path("sender").asText(), root.path("text").asText());
                });
                default -> { /* ignore */ }
            }
        } catch (Exception ex) {
            log.error("Packet 처리 오류", ex);
        }
    }

    private void initRoom(JsonNode root) {
        roomId        = root.path("roomId").asText();
        totalLoops    = root.path("totalLoops").asInt();  // 서버가 "loops" 가 아닌 "totalLoops" 로 보내줄 수 있으니 확인
        currentLoop   = 1;
        totalFocusSec = root.path("focusSec").asInt();
        totalBreakSec = root.path("breakSec").asInt();
        participants.clear();
        userMap.clear();
        for (JsonNode u : root.withArray("members")) {
            String id   = u.path("id").asText();
            String name = u.path("name").asText();
            String role = u.path("role").asText();
            userMap.put(id, name);
            participants.add(String.format("%s(%s) - %s", name, id, role));
        }
        // 기본 phase
        timerModel.setPhase("FOCUS");
        updatePhaseUI();
    }

    private void loadChatHistory(JsonNode arr) {
        chats.clear();
        for (JsonNode m : arr) {
            addChat(m.path("sender").asText(), m.path("content").asText());
        }
    }

    private void updatePhaseUI() {
        String phase = timerModel.getPhase();
        phaseLabel.setText(phase.equals("FOCUS") ? "Focus" : "Break");
        // 초기 시간 표시
        int sec = phase.equals("FOCUS") ? totalFocusSec : totalBreakSec;
        updateTimer(sec);
    }

    private void updateTimer(int remainingSec) {
        int mins = remainingSec / 60;
        int secs = remainingSec % 60;
        timeLabel.setText(String.format("%02d:%02d", mins, secs));
        double progress = remainingSec / (double)
                (timerModel.getPhase().equals("FOCUS") ? totalFocusSec : totalBreakSec);
        progressIndicator.setProgress(progress);
        cycleLabel.setText(String.format(
                "Cycle %d of %d · %d min focus · %d min break",
                currentLoop, totalLoops,
                totalFocusSec/60, totalBreakSec/60
        ));
    }

    private void sendChat() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        try {
            String payload = String.format(
                    "{\"roomId\":\"%s\",\"sender\":\"%s\",\"text\":\"%s\"}",
                    roomId,
                    UserSession.getInstance().getCurrentUser().getId(),
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
        String time  = LocalTime.now().format(TIME_FMT);
        String name  = userMap.getOrDefault(sender, sender);
        chats.add(String.format("%s  %s: %s", time, name, text));
        chatList.scrollTo(chats.size()-1);
    }

    @Override
    public void onError(Exception e) {
        log.error("네트워크 오류", e);
    }
}
