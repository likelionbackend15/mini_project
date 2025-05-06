/* 0) 스키마 */
CREATE DATABASE IF NOT EXISTS studybuddy
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE studybuddy;

/* 1) users – UUID(36) PK */
CREATE TABLE users (
  id           VARCHAR(50)   NOT NULL,   -- 사용자가 입력
  username     VARCHAR(100)  NOT NULL,   -- 별명 (중복 허용)
  hashed_pw    VARCHAR(255)  NOT NULL,
  created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  email        VARCHAR(100)  NOT NULL UNIQUE,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

/* 2) rooms – host_id 도 CHAR(36) */
CREATE TABLE rooms (
  room_id         CHAR(36)   NOT NULL,          -- UUID
  name            VARCHAR(100)   NOT NULL,
  host_id     VARCHAR(50)  NOT NULL,          -- users.id
  max_members     INT        NOT NULL,
  allow_mid_entry BOOLEAN    NOT NULL DEFAULT FALSE,
  focus_min       INT        NOT NULL,
  break_min       INT        NOT NULL,
  loops           INT        NOT NULL,
  password        VARCHAR(255),
  status          ENUM('OPEN','LOCKED','RUNNING','CLOSED') NOT NULL,
  created_at      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (room_id),
  FOREIGN KEY (host_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

/* 3) chat_messages – user_id 문자열 */
CREATE TABLE chat_messages (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  room_id    CHAR(36)    NOT NULL,
  user_id    VARCHAR(50) NOT NULL,
  message    TEXT        NOT NULL,
  created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id)      ON DELETE CASCADE,
  INDEX idx_chat_room (room_id),
  INDEX idx_chat_user (user_id)
) ENGINE=InnoDB;

/* 4) timer_logs – user_id 문자열 */
CREATE TABLE timer_logs (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  room_id    CHAR(36)    NOT NULL,
  user_id    VARCHAR(50) NOT NULL,
  loop_idx   INT         NOT NULL,
  focus_sec  INT         NOT NULL,
  break_sec  INT         NOT NULL,
  created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id)      ON DELETE CASCADE,
  INDEX idx_timer_room (room_id),
  INDEX idx_timer_user (user_id)
) ENGINE=InnoDB;

select * from users;


