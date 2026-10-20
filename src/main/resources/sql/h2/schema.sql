-- H2 内存演示模式 DDL（MODE=MySQL 兼容子集）
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS channel;
DROP TABLE IF EXISTS rate_rule;
DROP TABLE IF EXISTS order_main;
DROP TABLE IF EXISTS order_address;
DROP TABLE IF EXISTS order_track_event;
DROP TABLE IF EXISTS bill;
DROP TABLE IF EXISTS bill_detail;
DROP TABLE IF EXISTS sys_seq;
DROP TABLE IF EXISTS import_record;
DROP TABLE IF EXISTS import_error;

CREATE TABLE sys_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(100) NOT NULL,
  role        VARCHAR(20)  NOT NULL,
  customer_id BIGINT       NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customer (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_code VARCHAR(20)  NOT NULL UNIQUE,
  company_name  VARCHAR(100) NOT NULL,
  contact       VARCHAR(50),
  country       CHAR(2),
  status        TINYINT      NOT NULL DEFAULT 1,
  created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE channel (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  channel_code   VARCHAR(20) NOT NULL UNIQUE,
  channel_name   VARCHAR(50) NOT NULL,
  transport_type VARCHAR(20) NOT NULL,
  carrier        VARCHAR(50),
  eta_days       INT         NOT NULL DEFAULT 7,
  vol_divisor    INT         NOT NULL DEFAULT 5000,
  currency       CHAR(3)     NOT NULL DEFAULT 'USD',
  status         TINYINT     NOT NULL DEFAULT 1,
  created_at     DATETIME    DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rate_rule (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  channel_id     BIGINT        NOT NULL,
  dest_country   CHAR(2)       NOT NULL,
  first_weight   DECIMAL(6,3)  NOT NULL,
  first_price    DECIMAL(10,2) NOT NULL,
  step_weight    DECIMAL(6,3)  NOT NULL,
  step_price     DECIMAL(10,2) NOT NULL,
  fuel_rate      DECIMAL(5,2)  NOT NULL DEFAULT 0,
  reg_fee        DECIMAL(10,2) NOT NULL DEFAULT 0,
  currency       CHAR(3)       NOT NULL DEFAULT 'USD',
  effective_date DATE          NOT NULL,
  expired_date   DATE          NOT NULL,
  created_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (channel_id, dest_country)
);

CREATE TABLE order_main (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no        VARCHAR(32)  NOT NULL UNIQUE,
  customer_id     BIGINT       NOT NULL,
  channel_id      BIGINT       NOT NULL,
  status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
  version         INT          NOT NULL DEFAULT 0,
  idem_token      VARCHAR(64)  NULL UNIQUE,
  goods_desc      VARCHAR(500),
  pieces          INT          NOT NULL DEFAULT 1,
  actual_weight   DECIMAL(10,3),
  length_cm       DECIMAL(6,1),
  width_cm        DECIMAL(6,1),
  height_cm       DECIMAL(6,1),
  vol_weight      DECIMAL(10,3),
  billable_weight DECIMAL(10,3),
  freight         DECIMAL(12,2),
  currency        CHAR(3)      NOT NULL DEFAULT 'USD',
  created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_address (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id   BIGINT       NOT NULL,
  addr_type  VARCHAR(10)  NOT NULL,
  name       VARCHAR(50)  NOT NULL,
  phone      VARCHAR(30),
  country    CHAR(2)      NOT NULL,
  city       VARCHAR(50),
  street     VARCHAR(200),
  zip        VARCHAR(20),
  created_at DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_track_event (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id   BIGINT       NOT NULL,
  event_code VARCHAR(20)  NOT NULL,
  event_desc VARCHAR(200) NOT NULL,
  location   VARCHAR(100),
  event_time DATETIME     NOT NULL,
  operator   VARCHAR(50),
  created_at DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bill (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_no      VARCHAR(40)   NOT NULL UNIQUE,
  customer_id  BIGINT        NOT NULL,
  period       VARCHAR(7)    NOT NULL,
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  currency     CHAR(3)       NOT NULL DEFAULT 'USD',
  status       VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
  created_at   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (customer_id, period)
);

CREATE TABLE bill_detail (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_id    BIGINT        NOT NULL,
  order_id   BIGINT        NOT NULL,
  order_no   VARCHAR(32)   NOT NULL,
  goods_desc VARCHAR(500),
  freight    DECIMAL(12,2) NOT NULL,
  currency   CHAR(3)       NOT NULL DEFAULT 'USD',
  created_at DATETIME      DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_seq (
  name       VARCHAR(32) PRIMARY KEY,
  seq_value  BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_record (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name  VARCHAR(200) NOT NULL,
  operator   VARCHAR(50)  NOT NULL,
  total      INT NOT NULL,
  success    INT NOT NULL,
  failed     INT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_error (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id  BIGINT       NOT NULL,
  row_no     INT          NOT NULL,
  error_msg  VARCHAR(500) NOT NULL,
  created_at DATETIME     DEFAULT CURRENT_TIMESTAMP
);
