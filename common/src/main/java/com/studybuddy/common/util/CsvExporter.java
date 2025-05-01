package com.studybuddy.common.util;

import com.studybuddy.common.domain.TimerLog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * TimerLog 목록을 CSV로 변환하여 임시 파일로 저장합니다.
 */
public final class CsvExporter {

    private CsvExporter() {}

    public static Path exportRoomLogs(List<TimerLog> logs) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("logId,roomId,userId,loopIdx,focusSec,breakSec,ts\n");
        for (TimerLog log : logs) {
            sb.append(String.format("%d,%s,%d,%d,%d,%d,%s\n",
                    log.getLogId(),
                    log.getRoomId(),
                    log.getUserId(),
                    log.getLoopIdx(),
                    log.getFocusSec(),
                    log.getBreakSec(),
                    log.getTs().toString()
            ));
        }
        Path tempFile = Files.createTempFile("study_stats_", ".csv");
        Files.writeString(tempFile, sb.toString(), StandardCharsets.UTF_8);
        return tempFile;
    }
}
