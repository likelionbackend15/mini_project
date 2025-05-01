package com.studybuddy.client;

import com.studybuddy.client.net.ClientSocket;
import com.studybuddy.client.net.PacketListener;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.studybuddy.client.ui.LoginController;

import java.io.PrintWriter;
import java.net.Socket;

public class MainApp extends Application {
    private ClientSocket clientSocket;
    private PrintWriter  out;

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
            case ACK:
            case ERROR:
                // 로그인·방 생성 등 단순 ACK 처리 로직
                break;
            case LIST_ROOMS:
                // 예: 방 목록 화면 전환
                // forwardTo("/fxml/RoomListView.fxml", pkt);
                break;
            case CREATE_ROOM:
            case JOIN_ROOM:
                // 예: 스터디룸 화면 전환
                // forwardTo("/fxml/StudyRoomView.fxml", pkt);
                break;
            case STATS_VIEW:
            case DOWNLOAD_CSV:
                // 통계 화면
                // forwardTo("/fxml/StatsView.fxml", pkt);
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

    public static void main(String[] args) {
        launch(args);
    }
}
