package com.studybuddy.server;

import com.studybuddy.common.Packet;
import com.studybuddy.common.PacketType;
import com.studybuddy.common.domain.TimerLog;
import com.studybuddy.common.domain.User;
import com.studybuddy.server.dao.LogDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * 방 하나당 실행되는 뽀모도로 타이머
 * ─ 1초마다 남은 시간을 모든 클라이언트에게 알림
 * ─ FOCUS ↔ BREAK 단계를 자동 전환
 * ─ 루프가 끝날 때마다 DB에 TimerLog 기록
 */
public class PomodoroTimer {

    private static final Logger log = LoggerFactory.getLogger(PomodoroTimer.class);

    /* 스케줄러 한 개(1초 간격) */
    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "timer-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> tickJob;   // 1초 반복 작업
    private final RoomSession  session;
    private final LogDAO       logDao;

    private TimerState state   = TimerState.FINISHED;
    private int        loopIdx = 0;        // 1,2,3 …
    private int        remain  = 0;        // 남은 초

    public PomodoroTimer(RoomSession session, LogDAO logDao) {
        this.session = session;
        this.logDao  = logDao;
    }

    /* ---------- 공개 API ---------- */

    /** 집중 단계 시작 */
    public synchronized void startFocus(int seconds) {
        startPhase(TimerState.FOCUS, seconds);
    }

    /** 휴식 단계 시작 */
    public synchronized void startBreak(int seconds) {
        startPhase(TimerState.BREAK, seconds);
    }

    /** 강제 종료(방 폭파 등) */
    public void stop() {
        if (tickJob != null) tickJob.cancel(true);
        exec.shutdownNow();
        state = TimerState.FINISHED;
    }

    /* ---------- 내부 로직 ---------- */

    /**
     * 집중 단계 또는 휴식 단계를 시작합니다.
     *
     * @param next    다음 단계 (FOCUS 또는 BREAK)
     * @param seconds 해당 단계의 지속 시간(초)
     */
    private void startPhase(TimerState next, int seconds) {
        // 1) 이전에 예약된 틱 작업이 있으면 취소
        if (tickJob != null) {
            tickJob.cancel(false);
        }

        // 2) 집중 단계가 시작될 때마다 루프 인덱스 증가
        if (next == TimerState.FOCUS) {
            loopIdx++;  // 새 스터디 루프 진입!
        }

        // 3) 현재 상태와 남은 시간을 설정
        state  = next;     // 이제 FOCUS 또는 BREAK 상태
        remain = seconds;  // 남은 시간을 초기화

        // 4) 1초마다 tick() 메서드를 호출하도록 예약
        //    즉시 실행 시작(0초 뒤), 1초 간격으로 계속 실행
        tickJob = exec.scheduleAtFixedRate(
                this::tick,      // 매번 실행할 함수
                0,               // 첫 실행 지연 시간(초)
                1,               // 반복 주기(초)
                TimeUnit.SECONDS // 시간 단위
        );
    }


    /**
     * 1초마다 호출되어 남은 시간을 줄이고,
     * 모든 클라이언트에 현재 상태를 알려주며,
     * 시간이 다 되면 다음 단계로 전환하는 메서드
     */
    private void tick() {
        try {
            // 남은 시간을 1초 줄임
            remain--;

            //방 안 모든 클라이언트에 남은 시간과 현재 단계(Focus/Break) 알림
            session.broadcast(new Packet(
                    PacketType.TIMER_TICK,
                    "{\"remainingSec\":" + remain + ",\"phase\":\"" + state + "\"}"
            ));

            //시간이 0초 이하가 되면 단계 전환 로직 실행
            if (remain <= 0) {
                if (state == TimerState.FOCUS) {
                    // • 집중 단계가 끝났을 때 로그 저장
                    saveFocusLog();
                    // • 휴식 단계 시작 (설정된 분량*60초)
                    startBreak(session.getMeta().getBreakMin() * 60);
                } else {
                    // • 휴식 단계가 끝났을 때
                    //   – 아직 반복 횟수가 남으면 다시 집중 단계로
                    if (loopIdx < session.getMeta().getLoops()) {
                        startFocus(session.getMeta().getFocusMin() * 60);
                    } else {
                        //   – 반복이 모두 끝나면 종료 알림 보내고 타이머 정리
                        session.broadcast(new Packet(PacketType.TIMER_END, ""));
                        stop();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Timer tick error", e);
        }
    }

    /**
     * 집중 단계가 끝났을 때
     *  현재 방에 접속 중인 모든 사용자별로 로그를 DB에 남김
     */
    private void saveFocusLog() {
        // 1) 방에 들어 있는 모든 핸들러를 순회
        for (ClientHandler ch : session.getMembers()) {
            try {
                User u = ch.getUser();
                if (u == null) continue;  // 로그인 안 된 클라이언트 건너뛰기

                // 2) 각 사용자별 TimerLog 생성
                TimerLog logRow = new TimerLog(
                        null,
                        session.getMeta().getRoomId(),
                        u.getId(),                       // ★ 사용자 ID
                        loopIdx,
                        session.getMeta().getFocusMin() * 60,
                        0,
                        LocalDateTime.now()
                );
                // 3) DB에 저장
                logDao.save(logRow);
            } catch (Exception e) {
                log.error("TimerLog 저장 실패 for user", e);
            }
        }
    }
}
