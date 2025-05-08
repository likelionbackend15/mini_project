package com.studybuddy.client.ui;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.studybuddy.client.MainApp;
import com.studybuddy.client.model.UserSession;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.util.Optional;

public class MyInfoController  {

    @FXML private Label idLabel;
    @FXML private Label emailLabel;
    @FXML private Button changePasswordButton;
    @FXML private Button logoutButton;
    @FXML private Button deleteButton;

    private PrintWriter out;
    private MainApp app;
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
        // 사용자 정보 표시
        idLabel.setText(UserSession.getInstance().getCurrentUser().getId());
        emailLabel.setText(UserSession.getInstance().getCurrentUser().getEmail());
        changePasswordButton.setOnAction(e -> handleChangePassword());
        logoutButton.setOnAction(e -> handleLogout());
        deleteButton.setOnAction(e -> handleDeleteAccount());
    }

    private void handleChangePassword() {
        try {
            app.forwardTo("/fxml/ChangePasswordView.fxml",null);
        } catch (Exception e) {
            showError("비밀번호 변경 화면을 열 수 없습니다: " + e.getMessage());
        }
    }

    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("로그아웃 확인");
        confirmAlert.setHeaderText("로그아웃 하시겠습니까?");
        confirmAlert.setContentText("애플리케이션을 다시 사용하려면 로그인이 필요합니다.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            UserSession.getInstance().clear();


            try {
                app.forwardTo("/fxml/LoginView.fxml", null);
            } catch (Exception e) {
                showError("로그인 화면으로 전환할 수 없습니다: " + e.getMessage());
            }
        }
    }

    private void handleDeleteAccount() {


        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("계정 삭제");
        confirmAlert.setHeaderText("계정을 삭제하시겠습니까?");
        confirmAlert.setContentText("이 작업은 되돌릴 수 없으며 모든 데이터가 영구적으로 삭제됩니다.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                app.forwardTo("/fxml/DeleteAccountView.fxml",null);
            } catch (Exception e) {
                showError("계정 삭제 요청 실패: " + e.getMessage());
            }
        }
    }



    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("오류");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }


}