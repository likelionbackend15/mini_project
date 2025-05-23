package com.studybuddy.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.net.ClientSocket;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.util.JsonUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.PrintWriter;

public class MainApp extends Application {

    private ClientSocket clientSocket;
    private PrintWriter  out;

    /** 현재 화면 컨트롤러가 PacketListener 를 구현하면 여기에 보관 */
    private PacketListener currentListener;

    private Stage primaryStage;                      // → start()에서 저장
    private static final ObjectMapper mapper = JsonUtil.mapper();

    /* ==================== JavaFX 진입점 ==================== */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;            // ★ Stage 보관

        /* 1) 서버 연결 */
        clientSocket = new ClientSocket();
        clientSocket.connect("115.138.245.92", 12345);// 서버 테스팅용
//        clientSocket.connect("localhost", 12345); //로컬 테스트용
//        clientSocket.connect("192.168.123.101", 12345); //송치호 테스팅용
        out = clientSocket.getWriter();

        /* 2) 서버 패킷 수신 → routePacket() */
        clientSocket.addListener(new PacketListener() {
            @Override public void onPacket(Packet p) {
                Platform.runLater(() -> {
                    try { routePacket(p); }
                    catch (JsonProcessingException e) { e.printStackTrace(); }
                });
            }
            @Override public void onError(Exception e) {
                Platform.runLater(() ->
                        showAlert("네트워크 오류", e.getMessage()));
            }
        });

        /* 3) 첫 화면: Login */
        forwardTo("/fxml/LoginView.fxml", null);
    }

    /* ================= 화면 전환 & Listener 관리 =============== */

    /** 새 화면 컨트롤러가 PacketListener라면 여기로 등록 */
    public void addScreenListener(PacketListener l) {
        currentListener = l;
    }

    /** Packet 분배 (ACK → 화면전환, 실시간 이벤트 → currentListener) */
    private void routePacket(Packet pkt) throws JsonProcessingException {
        switch (pkt.type()) {
            /* 1) 성공 응답 */
            case ACK -> handleAck(pkt);

            /* 2) 실시간 이벤트(채팅·타이머) → 현재 화면 컨트롤러에만 전달 */
            case CHAT, TIMER_FOCUS_START, TIMER_BREAK_START, TIMER_TICK, TIMER_END -> {
                if (currentListener != null) currentListener.onPacket(pkt);
            }

            /* 3) 서버 오류 */
            case ERROR -> showAlert("서버 오류", pkt.payloadJson());

            /* 4) 기타 */
            default -> System.err.println("Unhandled packet: " + pkt.type());
        }
    }

    /** ACK 본문(action)에 따라 화면 이동 */
    private void handleAck(Packet pkt) {
        try {
            String action = mapper.readTree(pkt.payloadJson())
                    .path("action").asText();

            // ✅ 여기서 화면 전환 처리 추가
            switch (action) {
                case "JOIN_ROOM", "JOIN_PRIVATE" -> {
                    forwardTo("/fxml/PomodoroView.fxml", pkt);
                    return;
                }
                case "CREATE_ROOM" -> {
                    forwardTo("/fxml/RoomHostView.fxml", pkt); // ✅ 여기 추가
                    return;
                }
                default -> {
                    if (currentListener != null) {
                        currentListener.onPacket(pkt);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /** FXML → 컨트롤러 초기화 → Scene 전환 */
    public void forwardTo(String fxml, Packet firstPkt) {
        try {
            var url = getClass().getResource(fxml);
            if (url == null) {
                showAlert("리소스 못 찾음", fxml + " 경로를 확인하세요");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Scene scene = new Scene(loader.load());
            Object ctrl = loader.getController();

            // --- 리플렉션 기반 자동 주입 시작 ---
            for (var m : ctrl.getClass().getMethods()) {
                if (m.getName().equals("setWriter")
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == PrintWriter.class) {
                    m.invoke(ctrl, out);
                }
                if (m.getName().equals("setApp")
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == MainApp.class) {
                    m.invoke(ctrl, this);
                }
            }
            // --- 자동 주입 끝 ---

            // ✅ PacketListener 등록을 먼저!
            if (ctrl instanceof PacketListener pl) {
                addScreenListener(pl);
            }

            // ✅ 그 다음에 패킷 전달
            if (firstPkt != null) {
                try {
                    ctrl.getClass()
                            .getMethod("setInitData", Packet.class)
                            .invoke(ctrl, firstPkt);
                } catch (NoSuchMethodException ignored) {
                    try {
                        ctrl.getClass()
                                .getMethod("onPacket", Packet.class)
                                .invoke(ctrl, firstPkt);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /* 알림 */
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}
