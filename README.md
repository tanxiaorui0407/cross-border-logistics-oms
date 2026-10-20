# 跨境物流订单管理系统（Cross-Border Logistics OMS）

> 面向跨境货代公司与跨境卖家的订单管理系统，覆盖 **下单 → 计费 → 状态流转 → 轨迹追踪 → 对账** 的全生命周期。

## 技术栈（精准对齐后端 JD）

| 层 | 技术 | 说明 |
|---|---|---|
| 后端 | Spring Boot 2.7 + MyBatis-Plus 3.5 | RESTful API |
| 数据库 | MySQL 8 + Redis | Redis 缓存价卡 / 幂等 |
| 前端 | Thymeleaf + Bootstrap + jQuery + Ajax | 不引入 Vue，直击传统 Web 岗位 JD |
| 接口文档 | RESTful + Knife4j/springdoc | 访问 `/doc.html` |
| 工程 | JUnit5 + Mockito + GitHub Actions + Docker Compose | 单测 / CI / Linux 容器化部署 |

## 功能清单

### P0（已完成 · 核心闭环）
- **JWT 登录 + RBAC 三角色**：ADMIN（管理员）/ OPS（操作员）/ CUSTOMER（客户），不同角色订单可见范围不同
- **渠道与价卡**：物流渠道维护（国际快递/邮政小包/专线）、运价规则（首重续重、目的国、燃油费率、挂号费），价卡查询走 Redis 缓存
- **订单管理**：下单（幂等 token 防重复提交）、分页+多条件查询、详情、状态流转
- **运费试算**：目的国 + 重量 + 尺寸 → 多渠道比价，按价格排序（差异化亮点）
- **轨迹追踪**：状态机系统事件自动落轨迹 + OPS 手工录入，客户端时间线展示
- **状态机 + 乐观锁**：10 种状态集中式流转表校验；`UPDATE ... WHERE status=? AND version=?` 防并发覆盖

### P1（已完成）
- **Excel 批量导入**：客户批量下单（EasyExcel），行级校验 + 错误报告下载 + 导入留痕（import_record / import_error）
- **账单对账**：按月为客户聚合有效订单生成账单（bill + bill_detail），Excel 导出

### P2（规划中，可扩展）
- RabbitMQ + 邮件异步状态通知、延迟取消
- 快递100 / 17track 外部轨迹对接（适配器模式）

## 核心设计（面试弹药库）

### 1. 订单状态机（10 态）
```
DRAFT → SUBMITTED → PICKED_UP → IN_TRANSIT → CUSTOMS → DELIVERING → SIGNED
旁路：EXCEPTION（可恢复） / CANCELED / REJECTED
```
- **演进故事**：v1.0 用 if/else 散落业务代码 → 重构为 `Map<OrderStatus, Map<OrderStatus, Set<Role>>>` 集中式流转表，新增状态只改一张表
- **并发安全**：乐观锁 `UPDATE order_main SET status=?, version=version+1 WHERE id=? AND status=? AND version=?`，影响行数为 0 即抛"已被其他操作员变更"

### 2. 运费计算引擎（策略模式）
```
体积重 = 长 × 宽 × 高 / 系数（渠道可配，空运 5000 / 专线 8000）
计费重 = max(实重, 体积重)
计费重 ≤ 首重 → 运费 = 首重价
计费重 > 首重 → 运费 = 首重价 + ceil((计费重-首重)/续重单位) × 续重单价
最终运费 = 运费 × (1 + 燃油费率) + 挂号费
```
- 金额全程 **BigDecimal**（double 精度丢失是经典扣分点）
- 不同运输类型（EXPRESS/POST/LINE）体积重系数用 **策略模式** 拆分

### 3. 下单幂等
- 客户端传 `X-Idempotency-Token`，服务端 `idem_token` 唯一索引兜底，重复提交返回已有订单

### 4. RBAC + 数据可见性
- `@RequireRole` 注解 + JWT 拦截器；CUSTOMER 只能查询/操作自己的订单（服务层强制拼接 `customer_id` 条件）

### 5. 缓存降级
- 价卡查询走 `CacheService` 抽象（Redis / 进程内双实现），无 Redis 环境自动降级，业务层无感知

## 快速开始

### 方式一：MySQL + Redis（推荐，贴近生产）
```bash
# 1. 启动基础设施（首次会自动初始化表结构与种子数据）
docker compose up -d mysql redis

# 2. 本地运行应用（默认 mysql profile）
mvn spring-boot:run
# 或打包运行
mvn package -DskipTests && java -jar target/cross-border-logistics-oms-1.0.0.jar
```
访问：`http://localhost:8080/`（页面）｜`http://localhost:8080/doc.html`（API 文档）

### 方式二：一键全容器
```bash
docker compose --profile app up -d --build
```

### 方式三：H2 免环境演示（无 MySQL / Redis 也能跑）
```bash
java -jar target/cross-border-logistics-oms-1.0.0.jar --spring.profiles.active=h2
```
H2 控制台：`http://localhost:8080/h2-console`（JDBC URL: `jdbc:h2:mem:oms`，用户 `sa`，空密码）

### 演示账号（密码均为 `123456`）
| 账号 | 角色 | 说明 |
|---|---|---|
| admin | ADMIN | 维护价卡/用户/客户 |
| ops | OPS | 处理订单、录入轨迹 |
| alice | CUSTOMER | 客户1（深圳飞鱼贸易） |
| bob | CUSTOMER | 客户2（广州云帆电商） |

## 数据库设计（12 张表）

```
customer 1──N order_main 1──N order_track_event
channel  1──N rate_rule        order_main 1──2 order_address(寄件人/收件人)
channel  1──N order_main       customer 1──N bill 1──N bill_detail
sys_user（role=CUSTOMER 时关联 customer_id）
sys_seq（发号器）｜import_record / import_error（导入留痕）
```

## 关键 REST 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/auth/login | 登录 |
| POST | /api/orders | 创建订单（幂等 token 防重复提交） |
| GET | /api/orders?status=&keyword=&page= | 订单分页查询 |
| GET | /api/orders/{id} | 订单详情（含轨迹时间线） |
| POST | /api/orders/{id}/transition | 状态流转（状态机+乐观锁） |
| POST | /api/quote/preview | 运费试算（多渠道比价） |
| POST | /api/orders/import | Excel 批量导入 |
| GET | /api/bills/{id}/export | 账单导出 |

完整接口见 `/doc.html`。

## 测试与 CI

- 单元测试：状态机合法性、计费引擎（首重/续重 ceil/燃油/体积重/精度）、订单服务（幂等/乐观锁冲突/角色权限）、JWT、导入行级校验、账单聚合、价卡缓存
- GitHub Actions：`mvn test` + 打包，产物上传 artifact

## 目录结构

```
src/main/java/com/logistics/oms
├── common/        统一返回体、异常体系
├── config/        MyBatis-Plus、拦截器、Knife4j、缓存、演示数据初始化
├── security/      JWT、@RequireRole 拦截器
├── cache/         缓存抽象（Redis / 进程内）
├── module/
│   ├── auth/      登录认证
│   ├── user/      用户管理（ADMIN）
│   ├── customer/  客户管理
│   ├── channel/   渠道 + 价卡（Redis 缓存）
│   ├── quote/     运费试算（策略模式）
│   ├── order/     订单 + 状态机 + 轨迹
│   ├── imports/   Excel 批量导入
│   └── bill/      账单对账
src/main/resources/templates/  Thymeleaf 页面
sql/mysql/init/                 MySQL 建表 + 种子数据
src/main/resources/sql/h2/      H2 演示库脚本
```
