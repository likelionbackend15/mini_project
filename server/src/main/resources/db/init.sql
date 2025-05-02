-- 0) 스키마 생성 및 선택
CREATE DATABASE IF NOT EXISTS studybuddy
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE studybuddy;

-- 1) 사용자 테이블
CREATE TABLE users (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  username     VARCHAR(50)  NOT NULL UNIQUE,
  hashed_pw    VARCHAR(255) NOT NULL,
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  email        VARCHAR(100) NOT NULL UNIQUE,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 2) 방 메타데이터 테이블
CREATE TABLE rooms (
  room_id         CHAR(36)       NOT NULL,          -- UUID
  name            VARCHAR(100)   NOT NULL,
  host_id         BIGINT         NOT NULL,          -- users.id 참조
  max_members     INT            NOT NULL,
  allow_mid_entry BOOLEAN        NOT NULL DEFAULT FALSE,
  focus_min       INT            NOT NULL,
  break_min       INT            NOT NULL,
  loops           INT            NOT NULL,
  password        VARCHAR(255),                       -- nullable: private room
  status          ENUM('OPEN','LOCKED','RUNNING','CLOSED') NOT NULL,
  created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (room_id),
  FOREIGN KEY (host_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3) 채팅 메시지 테이블
CREATE TABLE chat_messages (
  id           BIGINT    NOT NULL AUTO_INCREMENT,
  room_id      CHAR(36)  NOT NULL,             -- rooms.room_id 참조
  user_id      BIGINT    NOT NULL,             -- users.id 참조
  message      TEXT      NOT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_chat_room (room_id),
  INDEX idx_chat_user (user_id)
) ENGINE=InnoDB;

-- 4) 타이머 로그 테이블
CREATE TABLE timer_logs (
  id           BIGINT    NOT NULL AUTO_INCREMENT,
  room_id      CHAR(36)  NOT NULL,             -- rooms.room_id 참조
  user_id      BIGINT    NOT NULL,             -- users.id 참조
  loop_idx     INT       NOT NULL,
  focus_sec    INT       NOT NULL,
  break_sec    INT       NOT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_timer_room (room_id),
  INDEX idx_timer_user (user_id)
) ENGINE=InnoDB;
