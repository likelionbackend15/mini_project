package com.studybuddy.client.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 서버에서 내려온 CSV 문자열을
 * 로컬에 파일로 저장하는 헬퍼.
 */
public final class CsvDownloader {
    private CsvDownloader() { /* 유틸이므로 인스턴스화 금지 */ }

    /**
     * 화면에 파일 저장 대화상자를 띄운 뒤, CSV 내용을 파일로 씁니다.
     *
     * @param owner 윈도우 핸들. null 이면 독립 윈도우로 띄움
     * @param csvData 유효한 CSV 포맷의 전체 문자열
     * @param defaultFileName 제안할 파일명 (예: "study_stats_weekly.csv")
     * @return 저장된 File 객체, 취소했거나 실패 시 null
     */
    public static File saveCsv(Window owner, String csvData, String defaultFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("CSV 파일로 저장");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV 파일", "*.csv")
        );
        chooser.setInitialFileName(defaultFileName);

        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return null; // 사용자가 취소
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(csvData);
            writer.flush();
            return file;
        } catch (IOException e) {
            System.err.println("CSV 저장 중 오류: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
