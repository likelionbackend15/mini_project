package com.studybuddy.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.model.UserSession;
import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

import java.io.PrintWriter;

public class LobbyController {
    @FXML private Text welcomeText;
    @FXML private Button createRoomButton;
    @FXML private Button listRoomsButton;

    @FXML private Button joinPrivateRoomButton;
    @FXML private Button myInfoButton;

    private MainApp app;
    private PrintWriter out;
    private final ObjectMapper mapper = new ObjectMapper();

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
        // welcomeText는 FXML에서 바인딩됩니다
        welcomeText.setText("환영합니다, " + UserSession.getInstance().getCurrentUser().getUsername());

        createRoomButton.setOnAction(e -> showCreateRoom());
        listRoomsButton.setOnAction(e -> showRoomList());

        joinPrivateRoomButton.setOnAction(e -> joinPrivateRoom());
        myInfoButton.setOnAction(e -> showMyInfo());
    }

    /** 방 만들기 화면으로 전환 */
    private void showCreateRoom() {
        app.forwardTo("/fxml/RoomCreateView.fxml", null);
    }

    /** 방 목록 요청 후 RoomListController가 처리 */
    private void showRoomList() {
//        Packet pkt = new Packet(PacketType.LIST_ROOMS, "");
//        try {
//            out.println(mapper.writeValueAsString(pkt));
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } // 송치호 임시수정, 완벽하게 수정 요청 드립니다!

        app.forwardTo("/fxml/RoomListView.fxml", null);
    }

    /** 비공개 방 입장 화면으로 전환 */
    private void joinPrivateRoom() {
        app.forwardTo("/fxml/PrivateRoomJoinView.fxml", null);
    }



    /** my info로 화면 전환  */
    private void showMyInfo() {
        app.forwardTo("/fxml/MyInfoView.fxml", null);
    }
}
