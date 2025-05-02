package com.studybuddy.client;

import com.studybuddy.client.net.ClientSocket;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import com.studybuddy.client.ui.LoginController;

import java.io.PrintWriter;
import java.net.Socket;

public class MainApp extends Application {
    private ClientSocket clientSocket;
    private PrintWriter  out;
    private PacketListener currentListener;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1) 서버 연결
        clientSocket = new ClientSocket();
        clientSocket.connect("localhost", 12345);
        out = clientSocket.getWriter();  // PrintWriter 반환 메서드 있다고 가정

        // 2) 패킷 리스너 등록 (백그라운드 스레드 → UI 스레드로 라우팅)
        clientSocket.addListener(new PacketListener() {
            @Override
            public void onPacket(Packet pkt) {
                Platform.runLater(() -> routePacket(pkt));
            }
            @Override
            public void onError(Exception ex) {
                Platform.runLater(() -> {
                    // 네트워크 오류 처리 (Alert 등)
                    System.err.println("Connection error: " + ex.getMessage());
                });
            }
        });


        // 3) 첫 화면: 로그인
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
        Scene scene = new Scene(loader.load());
        LoginController ctrl = loader.getController();
        // LoginController 에 서버 소켓과 PrintWriter 주입
        ctrl.setConnection(clientSocket.getSocket(), out);

        primaryStage.setTitle("StudyBuddy Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /** 서버로부터 받은 Packet 을 화면별 컨트롤러에 전달 */
    private void routePacket(Packet pkt) {
        PacketType type = pkt.type();
        switch (type) {
            // 로그인 요청/응답
            case LOGIN:
                removeCurrentListener();
                forwardTo("/fxml/LobbyView.fxml", pkt);
                break;

            // 회원가입 요청/응답
            case SIGNUP:
                showAlert("회원가입 성공", "로그인 후 이용해 주세요.");
                removeCurrentListener();
                forwardTo("/fxml/LoginView.fxml", pkt);
                break;

            // 방 목록 조회
            case LIST_ROOMS:
                removeCurrentListener();
                forwardTo("/fxml/RoomListView.fxml", pkt);
                break;

            // 방 생성, 공개 방 입장, 비공개 방 입장
            case CREATE_ROOM, JOIN_ROOM, JOIN_PRIVATE:
                removeCurrentListener();
                forwardTo("/fxml/StudyRoomView.fxml", pkt);
                break;

            // 로비로 복귀
            case BACK_TO_LOBBY:
                removeCurrentListener();
                forwardTo("/fxml/LobbyView.fxml", pkt);
                break;

            // 방 설정 변경, 방 잠금
            case MODIFY_ROOM, LOCK_ROOM:
                // 설정 UI 업데이트

            // 타이머 흐름
            case TIMER_FOCUS_START, TIMER_BREAK_START, TIMER_TICK, TIMER_END:
                // 타이머 진행 상태 없데이트

            // 채팅
            case CHAT:
                // 채팅 메서지 View에 추가

            // 통계
            case ROOM_STATS, STATS_VIEW, DOWNLOAD_CSV:
                removeCurrentListener();
                forwardTo("/fxml/StatsView.fxml", pkt);
                break;

            // 성공 응답
            case ACK:
                System.out.println("[ACK] 서버 성공 응답: " + pkt.payloadJson());
                break;

            // 오류 응답
            case ERROR:
                System.err.println("서버 에러: " + pkt.payloadJson());
                showAlert("서버 오류", pkt.payloadJson());
                break;

            default:
                System.err.println("Unhandled packet type: " + type);
        }
    }

    /**
     * FXML 로드 → 컨트롤러에 PrintWriter + Packet 전달 → Scene 전환
     */
    private void forwardTo(String fxmlPath, Packet pkt) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());

            // 컨트롤러 setWriter(), onPacket() 호출
            Object controller = loader.getController();
            controller.getClass().getMethod("setWriter", PrintWriter.class).invoke(controller, out);
            controller.getClass().getMethod("onPacket", Packet.class).invoke(controller, pkt);

            Stage stage = (Stage) scene.getWindow();
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 화면 전환 전에 리스너 제거*/
    private void removeCurrentListener() {
        if (currentListener != null) {
            clientSocket.removeListener(currentListener);
            currentListener = null;
        }
    }

    /* 알림 팝업창 */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait(); // 사용자가 "확인"을 누를 때까지 대기
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
