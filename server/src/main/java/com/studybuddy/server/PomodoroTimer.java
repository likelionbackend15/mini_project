package com.studybuddy.server;

import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.domain.TimerLog;
import com.studybuddy.server.dao.LogDAO;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 서버 사이드 뽀모도로 타이머.
 *  - 1초 단위 스케줄러
 *  - 남은 시간마다 TIMER_TICK 전송
 *  - 단계(FOCUS ↔ BREAK) 자동 전환
 *  - 루프별 TimerLog 저장
 */
public class PomodoroTimer {

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final RoomSession session;
    private final LogDAO logDao;

    private TimerState state = TimerState.FINISHED;
    private int loopIdx = 0;
    private int remainingSec = 0;

    public PomodoroTimer(RoomSession session, LogDAO logDao) {
        this.session = session;
        this.logDao  = logDao;
    }

    /** 집중 단계 시작 */
    public synchronized void startFocus(int seconds) {
        state = TimerState.FOCUS;
        remainingSec = seconds;
        loopIdx++;
        scheduleTick();
    }

    /** 휴식 단계 시작 */
    public synchronized void startBreak(int seconds) {
        state = TimerState.BREAK;
        remainingSec = seconds;
        scheduleTick();
    }

    /** 내부: 1초 후에 남은 시간 전송 및 상태 전환 */
    private void scheduleTick() {
        exec.schedule(() -> {
            try {
                remainingSec--;
                // 1️⃣ 남은 시간 브로드캐스트
                session.broadcast(new Packet(
                        PacketType.TIMER_TICK,
                        "{\"remainingSec\":" + remainingSec
                                + ",\"phase\":\"" + state + "\"}"
                ));

                // 2️⃣ 남은 시간이 0 이면 단계 전환
                if (remainingSec <= 0) {
                    if (state == TimerState.FOCUS) {
                        // 집중 종료 시 로그 저장
                        TimerLog log = new TimerLog(
                                null,
                                session.getMeta().getRoomId(),
                                /*userId=*/0L,  // TODO: per-user 기록이라면 loop별 사용자별 저장 로직으로 변경
                                loopIdx,
                                session.getMeta().getFocusMin() * 60,
                                0,
                                LocalDateTime.now()
                        );
                        logDao.save(log);
                        startBreak(session.getMeta().getBreakMin() * 60);
                    } else if (state == TimerState.BREAK) {
                        // 반복 횟수 남았으면 집중, 없으면 종료
                        if (loopIdx < session.getMeta().getLoops()) {
                            startFocus(session.getMeta().getFocusMin() * 60);
                        } else {
                            session.broadcast(new Packet(PacketType.TIMER_END, ""));
                            state = TimerState.FINISHED;
                            exec.shutdown();
                        }
                    }
                } else {
                    // 계속 틱 스케줄링
                    scheduleTick();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, TimeUnit.SECONDS);
    }
}
