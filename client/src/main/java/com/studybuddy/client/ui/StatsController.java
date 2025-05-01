package com.studybuddy.client.ui;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

/**
 * 서버가 보내준 JSON 통계 데이터를 받아서
 * BarChart, LineChart 에 렌더링합니다.
 */
public class StatsController {
    @FXML private BarChart<String, Number> focusChart;
    @FXML private LineChart<String, Number> messageChart;
    @FXML private Button backButton;
    @FXML private Text statusText;

    @FXML
    public void initialize() {
        backButton.setOnAction(e -> goBack());
    }

    private void goBack() {
        // TODO: LobbyController 로 전환
    }

    /** 서버가 보내준 JSON을 읽어 차트에 그려주는 헬퍼 메서드를 추가 */
}
