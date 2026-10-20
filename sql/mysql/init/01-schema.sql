-- ============================================================
-- 跨境物流订单管理系统  DDL (MySQL 8)
-- 9 张业务表 + sys_seq 发号器 + import_record/import_error 导入留痕 = 12 张表
-- ============================================================
USE oms;

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

-- 1. 系统用户（RBAC 三角色）
CREATE TABLE sys_user (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录名',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 散列',
  role        VARCHAR(20)  NOT NULL COMMENT 'ADMIN / OPS / CUSTOMER',
  customer_id BIGINT       NULL COMMENT 'role=CUSTOMER 时关联 customer.id',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_role (role)
) COMMENT='系统用户';

-- 2. 客户
CREATE TABLE customer (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_code VARCHAR(20)  NOT NULL UNIQUE COMMENT '客户编码',
  company_name  VARCHAR(100) NOT NULL COMMENT '公司名',
  contact       VARCHAR(50)  COMMENT '联系人',
  country       CHAR(2)      COMMENT 'ISO 国家码',
  status        TINYINT      NOT NULL DEFAULT 1,
  created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='客户';

-- 3. 物流渠道
CREATE TABLE channel (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  channel_code  VARCHAR(20) NOT NULL UNIQUE COMMENT '渠道编码',
  channel_name  VARCHAR(50) NOT NULL,
  transport_type VARCHAR(20) NOT NULL COMMENT 'EXPRESS快递 / POST邮政 / LINE专线',
  carrier       VARCHAR(50) COMMENT '承运商',
  eta_days      INT         NOT NULL DEFAULT 7 COMMENT '参考时效(天)',
  vol_divisor   INT         NOT NULL DEFAULT 5000 COMMENT '体积重系数:空运5000/专线8000',
  currency      CHAR(3)     NOT NULL DEFAULT 'USD',
  status        TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='物流渠道';

-- 4. 运价规则（价卡）
CREATE TABLE rate_rule (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  channel_id     BIGINT       NOT NULL,
  dest_country   CHAR(2)      NOT NULL COMMENT 'ISO 国家码,如 US',
  first_weight   DECIMAL(6,3) NOT NULL COMMENT '首重 kg,如 0.5',
  first_price    DECIMAL(10,2) NOT NULL COMMENT '首重价',
  step_weight    DECIMAL(6,3) NOT NULL COMMENT '续重单位 kg',
  step_price     DECIMAL(10,2) NOT NULL COMMENT '续重单价',
  fuel_rate      DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '燃油附加费率 %',
  reg_fee        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '挂号费',
  currency       CHAR(3)      NOT NULL DEFAULT 'USD',
  effective_date DATE         NOT NULL,
  expired_date   DATE         NOT NULL,
  created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_country (channel_id, dest_country)
) COMMENT='运价规则';

-- 5. 订单主表
CREATE TABLE order_main (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no       VARCHAR(32)  NOT NULL UNIQUE COMMENT '单号:SO+日期+序列',
  customer_id    BIGINT       NOT NULL,
  channel_id     BIGINT       NOT NULL COMMENT '物流渠道',
  status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '订单状态(状态机)',
  version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  idem_token     VARCHAR(64)  NULL UNIQUE COMMENT '幂等token(下单防重)',
  goods_desc     VARCHAR(500) COMMENT '品名/报关信息',
  pieces         INT          NOT NULL DEFAULT 1 COMMENT '件数',
  actual_weight  DECIMAL(10,3) COMMENT '实重 kg',
  length_cm      DECIMAL(6,1),
  width_cm       DECIMAL(6,1),
  height_cm      DECIMAL(6,1),
  vol_weight     DECIMAL(10,3) COMMENT '体积重=长*宽*高/系数',
  billable_weight DECIMAL(10,3) COMMENT '计费重=max(实重,体积重)',
  freight        DECIMAL(12,2) COMMENT '运费',
  currency       CHAR(3)       NOT NULL DEFAULT 'USD',
  created_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_customer (customer_id),
  KEY idx_status (status),
  KEY idx_created (created_at)
) COMMENT='订单主表';

-- 6. 订单地址（一单两址：寄件人/收件人）
CREATE TABLE order_address (
  id        BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id  BIGINT      NOT NULL,
  addr_type VARCHAR(10) NOT NULL COMMENT 'SENDER / RECEIVER',
  name      VARCHAR(50) NOT NULL,
  phone     VARCHAR(30),
  country   CHAR(2)     NOT NULL,
  city      VARCHAR(50),
  street    VARCHAR(200),
  zip       VARCHAR(20),
  created_at DATETIME   DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order (order_id)
) COMMENT='订单地址';

-- 7. 轨迹事件
CREATE TABLE order_track_event (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id   BIGINT       NOT NULL,
  event_code VARCHAR(20)  NOT NULL COMMENT '状态码/自定义事件码',
  event_desc VARCHAR(200) NOT NULL,
  location   VARCHAR(100) COMMENT '地点(可选)',
  event_time DATETIME     NOT NULL,
  operator   VARCHAR(50)  COMMENT '操作人(系统事件=SYSTEM)',
  created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order (order_id)
) COMMENT='订单轨迹事件';

-- 8. 账单主表
CREATE TABLE bill (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  bill_no     VARCHAR(40)  NOT NULL UNIQUE COMMENT '账单号:BI+账期+客户',
  customer_id BIGINT       NOT NULL,
  period      VARCHAR(7)   NOT NULL COMMENT '账期 yyyy-MM',
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  currency    CHAR(3)      NOT NULL DEFAULT 'USD',
  status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / PAID',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_customer_period (customer_id, period)
) COMMENT='账单';

-- 9. 账单明细
CREATE TABLE bill_detail (
  id        BIGINT PRIMARY KEY AUTO_INCREMENT,
  bill_id   BIGINT       NOT NULL,
  order_id  BIGINT       NOT NULL,
  order_no  VARCHAR(32)  NOT NULL,
  goods_desc VARCHAR(500),
  freight   DECIMAL(12,2) NOT NULL,
  currency  CHAR(3)      NOT NULL DEFAULT 'USD',
  created_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
  KEY idx_bill (bill_id)
) COMMENT='账单明细';

-- 10. 发号器（订单号/账单号序列）
CREATE TABLE sys_seq (
  name       VARCHAR(32) PRIMARY KEY,
  seq_value  BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='发号器';

-- 11. Excel 导入记录
CREATE TABLE import_record (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_name  VARCHAR(200) NOT NULL,
  operator   VARCHAR(50)  NOT NULL,
  total      INT NOT NULL,
  success    INT NOT NULL,
  failed     INT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='导入记录';

-- 12. 导入行级错误
CREATE TABLE import_error (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_id  BIGINT      NOT NULL,
  row_no     INT         NOT NULL COMMENT 'Excel 行号(含表头)',
  error_msg  VARCHAR(500) NOT NULL,
  created_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
  KEY idx_record (record_id)
) COMMENT='导入错误明细';
