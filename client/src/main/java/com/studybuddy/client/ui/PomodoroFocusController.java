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
import java.util.HashMap;
import java.util.Map;

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

    private PrintWriter out;
    private final TimerModel timerModel = new TimerModel();
    private final ObservableList<String> participants = FXCollections.observableArrayList();
    private final ObservableList<String> chats        = FXCollections.observableArrayList();

    private final Map<String,String> userMap = new HashMap<>();

    // 화면 초기화에 필요한 값
    private String roomId;
    private int totalFocusSec;
    private int totalLoops;
    private int currentLoop;
    private int focusMin, breakMin;
    private int totalBreakSec;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final Logger log =
            LoggerFactory.getLogger(PomodoroFocusController.class);

    @FXML
    public void initialize() {
        // 1) 리스트뷰 바인딩
        participantsList.setItems(participants);
        chatList.setItems(chats);

        // 2) 현재 사용자 미리보기 (방장)  표시
        String myId = UserSession.getInstance().getCurrentUser().getId();
        String me   = UserSession.getInstance().getCurrentUser().getUsername();
        participants.clear();
        userMap.clear();
        userMap.put(myId, me);
        participants.add(String.format("%s(%s) - Host", me, myId));

        // 버튼을 default 버튼으로 두고
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(e -> sendChat());

        // TextField 의 onAction 은 빈 상태로 둬서 ENTER 시 버튼만 한 번 클릭
        messageField.setOnAction(null);

        // 폰트·사이즈
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 48px;");
        cycleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
    }


    /** MainApp이 자동 주입 */
    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    /** RoomHostController에서 화면 전환 시 초기 패킷을 전달 */
    public void setInitData(Packet initPkt) {
        try {
            JsonNode root = JsonUtil.mapper().readTree(initPkt.payloadJson());
            roomId        = root.path("roomId").asText();

            // 서버가 준 초(seconds) 단위로 읽어서 저장
            totalFocusSec = root.path("focusSec").asInt();
            totalBreakSec = root.path("breakSec").asInt();      // ← 추가

            totalLoops    = root.path("totalLoops").asInt();
            currentLoop   = root.path("loopIdx").asInt();

            // 참가자 초기화
            participants.clear();
            for (JsonNode u : root.withArray("members")) {
                String id   = u.path("id").asText();
                String name = u.path("name").asText();
                String role = u.path("role").asText();
                userMap.put(id, name);
                participants.add(String.format("%s(%s) - %s", name, id, role));
            }

            // 타이머 초기 갱신
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
                // 서버 브로드캐스트된 남은 시간 갱신
                case TIMER_TICK -> Platform.runLater(() ->
                        updateTimer(root.path("remainingSec").asInt())
                );

                // 채팅
                case CHAT -> Platform.runLater(() ->
                        addChat(root.path("sender").asText(),
                                root.path("text").asText())
                );

                default -> { /* 무시 */ }
            }
        } catch (Exception ex) {
            log.error("패킷 처리 오류", ex);
        }
    }

    private void updateTimer(int remainingSec) {
        // 1) 남은 시간 MM:SS 표시
        int mins = remainingSec / 60;
        int secs = remainingSec % 60;
        timeLabel.setText(String.format("%02d:%02d", mins, secs));

        // 2) 프로그레스 인디케이터 (focus total 대비)
        progressIndicator.setProgress((double) remainingSec / totalFocusSec);

        // 3) 사이클·포커스·브레이크 표시
        int focusMin = totalFocusSec / 60;
        int breakMin = totalBreakSec / 60;
        cycleLabel.setText(String.format(
                "Cycle %d of %d · %d min focus · %d min break",
                currentLoop, totalLoops, focusMin, breakMin
        ));
    }

    private void sendChat() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        try {
            String sender = UserSession.getInstance().getCurrentUser().getId();
            String payload = String.format(
                    "{\"roomId\":\"%s\",\"sender\":\"%s\",\"text\":\"%s\"}",
                    roomId, sender, text
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
        String uname = userMap.getOrDefault(sender, "");

        String disp = uname.isEmpty() ? sender : String.format("%s(%s)", uname, sender);
        chats.add(String.format("%s  %s: %s", time, disp, text));
        // 스크롤을 마지막으로
        chatList.scrollTo(chats.size() - 1);
    }

    @Override
    public void onError(Exception e) {
        log.error("네트워크 오류", e);
    }
}
