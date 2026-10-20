-- ============================================================
-- 种子数据：渠道 / 价卡 / 客户 / 演示订单（账号由 DataInitializer 创建）
-- 默认演示账号: admin / ops / alice / bob  密码均为 123456
-- ============================================================
USE oms;

-- 物流渠道
INSERT INTO channel (id, channel_code, channel_name, transport_type, carrier, eta_days, vol_divisor, currency) VALUES
(1, 'EXPRESS', '国际快递',     'EXPRESS', 'DHL/UPS',     5,  5000, 'USD'),
(2, 'POST',    '邮政小包',     'POST',    '中国邮政',    20, 5000, 'USD'),
(3, 'LINE',    '中欧专线',     'LINE',    '中欧专线',    12, 8000, 'USD');

-- 运价规则（目的国: US / GB / DE / FR）
INSERT INTO rate_rule (channel_id, dest_country, first_weight, first_price, step_weight, step_price, fuel_rate, reg_fee, currency, effective_date, expired_date) VALUES
(1, 'US', 0.5, 18.00, 0.5,  8.00, 5.00, 0.00, 'USD', '2026-01-01', '2026-12-31'),
(1, 'GB', 0.5, 20.00, 0.5,  9.00, 5.00, 0.00, 'USD', '2026-01-01', '2026-12-31'),
(1, 'DE', 0.5, 19.00, 0.5,  8.50, 5.00, 0.00, 'USD', '2026-01-01', '2026-12-31'),
(1, 'FR', 0.5, 21.00, 0.5,  9.50, 5.00, 0.00, 'USD', '2026-01-01', '2026-12-31'),
(2, 'US', 0.1,  8.00, 0.1,  0.90, 0.00, 0.00, 'USD', '2026-01-01', '2026-12-31'),
(2, 'GB', 0.1,  9.00, 0.1,  1.00, 0.00, 0.00, 'USD', '2026-01-01', '2026-12-31'),
(2, 'DE', 0.1,  9.50, 0.1,  1.05, 0.00, 0.00, 'USD', '2026-01-01', '2026-12-31'),
(2, 'FR', 0.1, 10.00, 0.1,  1.10, 0.00, 0.00, 'USD', '2026-01-01', '2026-12-31'),
(3, 'US', 1.0, 25.00, 1.0, 10.00, 3.00, 8.00, 'USD', '2026-01-01', '2026-12-31'),
(3, 'GB', 1.0, 27.00, 1.0, 11.00, 3.00, 8.00, 'USD', '2026-01-01', '2026-12-31'),
(3, 'DE', 1.0, 26.00, 1.0, 10.50, 3.00, 8.00, 'USD', '2026-01-01', '2026-12-31'),
(3, 'FR', 1.0, 28.00, 1.0, 11.50, 3.00, 8.00, 'USD', '2026-01-01', '2026-12-31');

-- 客户
INSERT INTO customer (id, customer_code, company_name, contact, country) VALUES
(1, 'CUST001', '深圳飞鱼贸易有限公司', 'Alice Zhang', 'US'),
(2, 'CUST002', '广州云帆电子商务',     'Bob Li',      'GB');

-- 演示订单（3 单，覆盖不同状态，方便演示角色可见性/状态机/账单）
INSERT INTO order_main (id, order_no, customer_id, channel_id, status, version, goods_desc, pieces, actual_weight, length_cm, width_cm, height_cm, vol_weight, billable_weight, freight, currency, created_at) VALUES
(1, 'SO202610180001', 1, 1, 'SUBMITTED', 0, '蓝牙耳机',      1, 0.500, 20.0, 15.0, 10.0, 0.600, 0.600,  27.30, 'USD', '2026-10-18 09:30:00'),
(2, 'SO202610180002', 1, 2, 'IN_TRANSIT', 0, '数据线',       2, 0.150, 10.0,  8.0,  5.0, 0.080, 0.150,   8.90, 'USD', '2026-10-18 10:00:00'),
(3, 'SO202610190001', 2, 3, 'DELIVERING', 0, '男女同款卫衣', 1, 1.500, 40.0, 30.0, 20.0, 3.000, 3.000,  58.47, 'USD', '2026-10-19 11:20:00');

INSERT INTO order_address (order_id, addr_type, name, phone, country, city, street, zip) VALUES
(1, 'SENDER', 'Alice Zhang',   '+86-755-88886666', 'CN', '深圳', '南山区科技园1号',       '518000'),
(1, 'RECEIVER', 'John Smith',  '+1-212-555-0100',  'US', 'New York', '5th Ave 1001',     '10001'),
(2, 'SENDER', 'Alice Zhang',   '+86-755-88886666', 'CN', '深圳', '南山区科技园1号',       '518000'),
(2, 'RECEIVER', 'Emma Brown',  '+1-310-555-0142',  'US', 'Los Angeles', 'Sunset Blvd 88', '90028'),
(3, 'SENDER', 'Bob Li',        '+86-20-66668888',  'CN', '广州', '天河区珠江新城2号',     '510000'),
(3, 'RECEIVER', 'Oliver Wang', '+44-20-7946-0958', 'GB', 'London', 'Baker Street 221B',  'NW1 6XE');

INSERT INTO order_track_event (order_id, event_code, event_desc, location, event_time, operator) VALUES
(1, 'DRAFT',     '订单已创建', NULL,           '2026-10-18 09:30:00', 'SYSTEM'),
(1, 'SUBMITTED', '订单已提交', NULL,           '2026-10-18 09:30:05', 'alice'),
(2, 'DRAFT',     '订单已创建', NULL,           '2026-10-18 10:00:00', 'SYSTEM'),
(2, 'SUBMITTED', '订单已提交', NULL,           '2026-10-18 10:00:10', 'alice'),
(2, 'PICKED_UP', '包裹已揽收', '深圳仓',       '2026-10-18 14:20:00', 'ops'),
(2, 'IN_TRANSIT','运输中',     '香港国际机场', '2026-10-19 08:15:00', 'ops'),
(3, 'DRAFT',     '订单已创建', NULL,           '2026-10-19 11:20:00', 'SYSTEM'),
(3, 'SUBMITTED', '订单已提交', NULL,           '2026-10-19 11:20:10', 'bob'),
(3, 'PICKED_UP', '包裹已揽收', '广州仓',       '2026-10-19 15:00:00', 'ops'),
(3, 'IN_TRANSIT','运输中',     '比利时列日机场','2026-10-20 02:30:00', 'ops'),
(3, 'CUSTOMS',   '清关中',     '比利时列日',   '2026-10-20 09:10:00', 'ops'),
(3, 'DELIVERING','派送中',     '伦敦',         '2026-10-20 14:40:00', 'ops');

-- 发号器初始值（已用 3 个订单号）
INSERT INTO sys_seq (name, seq_value) VALUES ('order_no', 3);
