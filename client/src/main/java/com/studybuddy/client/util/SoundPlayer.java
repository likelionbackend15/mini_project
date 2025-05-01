package com.studybuddy.client.util;

import javafx.scene.media.AudioClip;

/**
 * 간단한 효과음 재생기.
 * JavaFX AudioClip 을 사용해 리소스로 포함된 사운드를 재생
 */
public final class SoundPlayer {
    private static final String CLICK_SOUND = "/sounds/click.mp3";
    private static final String BEEP_SOUND  = "/sounds/beep.mp3";

    private SoundPlayer() { /* 유틸이므로 인스턴스화 금지 */ }

    /**
     * 버튼 클릭 등 UI 이벤트용 클릭 사운드 재생
     */
    public static void playClick() {
        play(CLICK_SOUND);
    }

    /**
     * 타이머 시작/종료용 비프음 재생
     */
    public static void playBeep() {
        play(BEEP_SOUND);
    }

    /**
     * 지정된 리소스 경로의 사운드를 재생
     *
     * @param resourcePath classpath 기준 리소스 경로 ("/sounds/xxx.mp3")
     */
    private static void play(String resourcePath) {
        try {
            AudioClip clip = new AudioClip(
                    SoundPlayer.class.getResource(resourcePath).toExternalForm()
            );
            clip.play();
        } catch (Exception e) {
            System.err.println("Sound play error: " + resourcePath);
            e.printStackTrace();
        }
    }
}
