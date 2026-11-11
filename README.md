# Mini Mall 商城后端

用于系统学习的单商家自营实物商城。Java 21、Spring Boot 4、多模块单体，MyBatis-Plus 持久化，MySQL 保存交易事实，Redis 保存登录会话与商品详情缓存。支付、退款和物流使用本地模拟渠道，真实渠道通过 `PaymentGateway`、`ShippingGateway` 接入。

**交付状态：业务源码、测试源码、数据库迁移和演示资料已编写。尚未执行项目编译、测试、容器启动或完整 HTTP 演示，不能据此认定运行验收通过。** 详见 [验收记录](docs/verification/验收记录.md)。

## 功能与边界

- 买家：注册登录、个人资料、地址、商品检索、领券、购物车、结算试算、幂等下单、模拟支付、订单取消、物流查询、确认收货、售后退货及退款查询。
- 后台：管理员与角色权限、会员启停、分类品牌规格、SPU/SKU、库存调整、满减券、订单查询、整单发货、售后审核验收、统计、操作审计、失败任务人工重试。
- 一致性：数据库事务与行锁、库存流水去重、订单快照、固定退款业务号、通知验签与去重、可恢复任务租约、支付与关单竞态补偿。
- 默认规则：人民币整数分、单仓、单张满减券、整单发货与退款、30 分钟支付期限、收货后 7 天内申请售后、2 小时登录会话。默认运费 0，可用 `MALL_FREIGHT_FEN` 设置统一运费。
- 暂不实现：前端、多商家、拆单、部分退款、会员积分、秒杀、真实资金渠道、微服务、消息队列或搜索引擎。

## 模块

| 模块 | 职责 | 依赖的业务模块 |
| --- | --- | --- |
| mall-common | 错误、分页、身份契约、MyBatis 工具 | 无 |
| mall-user | 会员、地址 | 无 |
| mall-catalog | 商品、分类规格、库存 | 无 |
| mall-marketing | 优惠券发放与核销 | 无 |
| mall-trade | 购物车、结算、订单、支付、物流、售后、退款、任务 | user、catalog、marketing |
| mall-admin | 管理 API、RBAC、报表、审计 | user、catalog、marketing、trade |
| mall-app | 启动、基础设施装配、本地数据、集成测试 | 全部 |

业务模块均依赖 common。跨模块只调用应用服务，不导入对方的 persistence 包。只有 mall-app 生成可执行 JAR。

## 本地准备

需要 JDK 21、Docker Desktop 和 PowerShell。仓库带 Maven Wrapper，无需预装 Maven。首先确认 Docker 正在运行，切换到仓库根目录。

```powershell
Copy-Item .env.example .env
# 编辑 .env，替换所有密码和开发密钥，然后：
.\scripts\import-env.ps1
.\scripts\check-ports.ps1
```

`.env` 已被 Git 忽略。学习环境可使用各自独立的 32 位以上随机字母、数字、下划线密码，避免不同环境文件解析器对 `$`、引号和内联注释的处理差异。`MALL_SIMULATION_KEY` 至少 32 个 UTF-8 字节，账号密码至少 8 个字符且满足代码中的长度限制。脚本仅导入当前 PowerShell 进程，不执行环境文件中的代码。

| 配置 | 默认值 / 作用 |
| --- | --- |
| MYSQL_PORT / REDIS_PORT / SERVER_PORT | 13306 / 16379 / 18080 |
| MYSQL_DATABASE / MYSQL_USER | mini_mall / mall |
| MYSQL_PASSWORD / MYSQL_ROOT_PASSWORD | 必须自行设置，分别用于应用账户与管理账户 |
| REDIS_PASSWORD | 必须自行设置 |
| MALL_SIMULATION_ENABLED | local 下明确设 true 才启用模拟渠道 |
| MALL_SIMULATION_KEY | 开发操作密钥，同时用于本地渠道通知 HMAC |
| MALL_SEED_ENABLED | local 下设 true 初始化管理员和演示数据 |
| MALL_ADMIN_PASSWORD | 首次初始化 admin 的密码 |
| MALL_DEMO_PASSWORD | 首次初始化 demo_buyer 的密码 |
| MALL_JOBS_ENABLED | 默认 true；关闭后异步支付、物流和退款不会自动推进 |

种子初始化使用业务服务，按 `demo-v1` 标识只执行一次；重启不会恢复库存、重新上架或覆盖密码。数据库迁移只创建结构与角色权限，不含任何固定密码。不要预先手动注册 `demo_buyer`，以免首次初始化产生用户名冲突。

## 启动与停止

以下是需要实际运行时执行的命令；本次交付没有执行这些命令。

```powershell
# 只启动本项目 MySQL 和 Redis，保留现有其他项目服务。
docker compose up -d mysql redis
docker compose ps

# 构建会编译项目；skipTests 仅跳过测试执行。
.\mvnw.cmd -pl mall-app -am package -DskipTests
if ($LASTEXITCODE -ne 0) { throw '构建失败，先修复再启动' }
java -jar .\mall-app\target\mall-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=local
```

首次启动 Flyway 顺序执行 `mall-app/src/main/resources/db/migration` 中的迁移。应用运行时，在另一个已导入配置的 PowerShell 中检查：

```powershell
Invoke-RestMethod http://127.0.0.1:18080/actuator/health
.\scripts\smoke.ps1
```

Swagger UI：`http://127.0.0.1:18080/swagger-ui/index.html`。接口分买家、管理、渠道三组；用 Bearer Token 调用业务 API，开发接口使用 `X-Dev-Key`。请求示例见 [HTTP 目录](docs/http)。`smoke.ps1` 每次创建唯一买家、商品和券，执行购买→付款→发货→收货→退货→退款，断言金额、库存、券和终态。它仅访问本机指定实例，不编译、不启动容器、不删除演示数据，也不打印密码或令牌。

本地 Java 进程用 Ctrl+C 停止。基础服务可执行：

```powershell
docker compose stop
```

该命令保留数据卷。不要使用 `docker compose down -v` 来排查普通启动失败，它会删除本项目的数据。

可选应用容器模式：先构建 JAR，再执行 `docker compose --profile app up -d --build`。先停止本地 Java，避免端口冲突。Dockerfile 以非 root 用户运行；容器内部端口固定 18080，外部端口使用 `SERVER_PORT`。此模式也使用 local 模拟渠道。

## 验证命令

```powershell
# 不编译、不启动服务
.\scripts\check-static.ps1

# 以下命令会编译并运行测试，需要运行阶段授权
.\mvnw.cmd test
.\mvnw.cmd -Pintegration verify
# 只执行一个集成用例；仍会编译相关模块
.\mvnw.cmd -pl mall-app -am -Pintegration -Dit.test=MallJourneyIT -Dfailsafe.failIfNoSpecifiedTests=false verify
```

单元测试以 `Test` 结尾，集成测试以 `IT` 结尾；只执行 `test` 不会运行 IT。Failsafe 报告在 `mall-app/target/failsafe-reports`，Surefire 报告在各模块 `target/surefire-reports`。集成测试使用独立 Testcontainers MySQL/Redis、随机 HTTP 端口及独立数据，不连接 `.env` 开发库。Redis 故障用例只暂停自己的测试容器，并在 finally 中恢复；不启用 JUnit 并行执行。

## 异步流程与故障排查

支付创建返回 202，仅表示请求受理；轮询 `/api/v1/payments/{paymentNo}`，获得模拟支付参数后再触发开发支付接口。取消订单返回 202，必须轮询订单到 `CLOSED` 或 `PAID`；渠道结果未知时库存保持占用，防止已付款订单被错误释放。已关闭订单发生异常收款会异步整单退款，订单仍为 CLOSED。

售后审核或验收后进入 REFUNDING，轮询售后到 COMPLETED，并检查退款单 SUCCEEDED。只有渠道退款确认成功后才返库，已使用优惠券不返还。物流签收轨迹不会替用户确认收货。

`POST /dev/v1/payments/{paymentNo}/failure` 模拟一次失败尝试，渠道记录 FAILED 并发送正常验签通知；订单仍待支付、库存继续占用。在有效期内重复获取原支付单参数，使用同一支付号调用 `/success` 可演示重试成功。成功或关闭终态不能改为失败。

任务由 MySQL 表 `integration_task` 驱动：READY→RUNNING→DONE；租约过期可重领，旧租约不能提交；失败按 5/15/60/300/900 秒重试，继续失败转 MANUAL。未知渠道结果正常轮询不累计失败次数，连续等待 24 小时转 MANUAL。人工重试会重新计算等待期限，仍使用原业务号。

| 现象 | 检查方法 |
| --- | --- |
| 端口占用 | 运行 check-ports.ps1，修改 `.env` 端口并重新导入 |
| 数据库连接失败 | 检查 compose ps 的健康状态；已有 MySQL 数据卷不会因改 `.env` 自动改密码 |
| 401 / 403 | 检查会话是否过期、账号是否禁用、角色权限是否足够；会员与管理员令牌分离 |
| 429 | 登录/注册限流生效，等待窗口结束，避免循环登录 |
| 409 | 检查商品价格/库存/券变化，重新试算；幂等键不得复用为不同请求 |
| 503 | 检查 Redis/渠道；认证在 Redis 故障时拒绝请求，商品详情缓存失败可回源 |
| 退款或取消迟迟未完成 | 管理端查看 `/admin/v1/tasks`，核对渠道状态与业务号，再重试 MANUAL 任务 |
| Flyway 校验失败 | 不修改已经应用的迁移，追加新版本；不要删除迁移记录掩盖问题 |

真实接入须实现签名验证、查单、关单、退款幂等、商户与金额校验以及网络超时处理，再调整渠道装配。当前 prod 配置会拒绝启动，防止把模拟资金功能部署成真实服务；`prod` 与 `local` 同时启用也不会开放开发路由。

## 学习入口

从 [阅读路线](docs/learning/00-阅读路线.md) 开始，每章先看业务问题，再看服务、SQL、测试和 HTTP 示例。[设计](docs/superpowers/specs/2026-11-11-mini-mall-design.md) 记录业务边界，[实施计划](docs/superpowers/plans/2026-11-11-mini-mall-implementation.md) 保留任务与运行验收要求。

固定版本为 Spring Boot 4.1.2、MyBatis-Plus 3.5.18、springdoc 3.2.0、MySQL 8.4.13、Redis 7.4.12-alpine、Maven 3.9.16。外部依赖版本固定；本项目的 `1.0.0-SNAPSHOT` 只是当前开发版本标识。
