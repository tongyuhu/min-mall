# Mini Mall 商城后端实施计划

> **执行者必读：**逐任务实现时必须使用 `superpowers:subagent-driven-development`（用户选择子代理方式时）或 `superpowers:executing-plans`（当前会话直接实现时）。以下复选框记录实际进度；未执行的验证不得勾选。

**目标：**交付一套用于 Java 后端系统学习的单商家实物商城，覆盖买家、管理端和完整交易闭环。

**架构：**Maven 七模块单体，只有一个 Spring Boot 启动入口。MySQL 保存业务事实，Redis 提供会话、限流和查询缓存；跨模块通过应用服务调用，订单使用同一数据源本地事务。支付和物流通过可替换接口接入持久化模拟渠道。

**技术栈：**Java 21、Spring Boot 4、Spring MVC、Spring Security、MyBatis-Plus、MySQL、Redis、Flyway、springdoc、JUnit、Testcontainers。

**设计依据：**[已批准设计](../specs/2026-11-11-mini-mall-design.md)，用户于 2026-11-11 批准设计和默认范围。用户已批准并选择当前会话按顺序实现。任务1–20的源码及资料已准备，所有编译和运行验证仍未执行；实际记录见 docs/verification/验收记录.md。下方复选框保留原验收要求，未获运行证据的复合步骤不勾选。

## 全局约束

- 使用 Java 21；Spring Boot 4 稳定版本；禁止 SNAPSHOT 和静默降级 Boot 3。
- 七模块及依赖方向严格遵循设计第 5 节；禁止跨模块访问 Mapper、数据库实体或写入他方表。
- 金额使用 BIGINT 分，Java 使用 long 并检查溢出；ID 对外为字符串；时间存 UTC，接口使用带时区的 ISO 8601。
- 人民币、一个仓库、固定运费默认 0 元；一个订单只用一张满减券；整单发货、整单退款；收货后 7 天售后期限。
- 会话有效期 2 小时，不滑动续期；分页最大 100；订单默认 30 分钟到期。
- 支付是否及时，以渠道实际收款时间不晚于截止时间为准；本地已关闭订单不重新激活。
- 网络调用放在数据库事务外；待执行任务与业务状态同事务落库；关键变更通过唯一约束和状态条件去重。
- 文档、说明、注释和提交信息使用中文，类名、字段名、协议与依赖坐标使用标准英文。
- 不引入前端、微服务、消息队列、独立搜索引擎、真实支付账号或生产部署。
- 未明确授权前不编译、不打包、不运行会触发编译的测试；本轮仅编写文档，不启动服务、不拉取镜像、不安装依赖。

## 重点审查输入与故障

除设计中的正常闭环外，以下五类输入纳入任务测试：

1. 超大金额与数量、负数、重复 SKU：拒绝溢出和非法数量，归并后重新限额，金额尾差不丢失。归属任务 7、10、11。
2. 同一幂等键并发提交不同内容，以及新键重复回调：不得产生两笔订单或二次扣库存。归属任务 11、14。
3. 禁用账号或改密码后旧令牌仍在 Redis，以及 Redis 故障：旧身份不能继续交易，不得降级放行。归属任务 4、5。
4. 回调先于发起支付响应、旧任务在租约过期后返回：终态不得倒退，旧执行者不得覆盖新结果。归属任务 12、14、15。
5. 售后、发货和确认收货同时操作，以及七天边界：只有合法状态迁移成功，退款和返库最多一次。归属任务 16、17、18。

## 版本锁定与核对依据

以下为实施基线，不声称已在本机完成依赖解析或启动验证：

| 项目 | 固定选择 | 管理方式 |
| --- | --- | --- |
| Java | 21 | `maven.compiler.release=21` |
| Spring Boot | 4.1.2 | 父 POM `spring-boot-starter-parent` |
| MyBatis-Plus | 3.5.18 | `mybatis-plus-spring-boot4-starter` 和 `mybatis-plus-jsqlparser` 同版本 |
| springdoc | 3.2.0 | `springdoc-openapi-starter-webmvc-ui` |
| Maven | 3.9.16 | Wrapper 固定下载版本并校验官方校验和 |
| MySQL | `mysql:8.4.13` | Compose 和测试使用同一标签 |
| Redis | `redis:7.4.12-alpine` | Compose 和测试使用同一标签 |
| Flyway、MySQL 驱动、Security、JUnit、Testcontainers | Boot 4.1.2 BOM 管理 | 不单独覆盖；包含 `flyway-mysql` 和 Testcontainers MySQL 模块 |

核对日期：2026-11-11。依据：[Boot 依赖表](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html)、[MyBatis-Plus 安装](https://baomidou.com/getting-started/install/)、[springdoc 3.2.0 发布说明](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.2.0)、[Maven 发布历史](https://maven.apache.org/docs/history.html)、[MySQL 官方镜像清单](https://github.com/docker-library/official-images/blob/master/library/mysql)、[Redis 官方镜像清单](https://github.com/docker-library/official-images/blob/master/library/redis)。

任务 1 必须检查精确坐标和镜像 manifest 可获取；如发现官方清单与仓库不一致，记录证据后只调整同代稳定补丁版本并更新本表。Boot 的 Jackson、测试模块和 Testcontainers 坐标以该 BOM 为准，禁止复制 Boot 3 示例强行覆盖依赖。

## 文件布局、数据迁移与接口约定

工作目录为 `C:\Users\1\workspace\mini-mall`，沿用当前 checkout。下文文件路径相对此目录。为避免重复，`J(m,p)` 精确表示 `m/src/main/java/com/minimall/p`，`T(m,p)` 表示 `m/src/test/java/com/minimall/p`；`R` 表示 `mall-app/src/main/resources`。括号内列出的每个文件均为独立文件，执行者必须按这一规则展开路径，不把简写写入代码。

| 位置 | 责任 |
| --- | --- |
| `pom.xml`、七模块各自的 `pom.xml` | 版本、依赖方向、单元测试与集成测试分离 |
| `mall-common` 下 `common/api`、`common/auth`、`common/config` | API 错误、身份契约、时钟和技术配置 |
| `mall-user` 下 `user/account`、`user/address` | 买家账户和收货地址 |
| `mall-catalog` 下 `catalog/product`、`catalog/stock` | 商品管理、可售查询和库存原子变化 |
| `mall-marketing` 下 `marketing/coupon` | 优惠券领取、占用与核销 |
| `mall-trade` 下 `trade/cart`、`trade/checkout`、`trade/order`、`trade/payment`、`trade/task`、`trade/shipping`、`trade/aftersale`、`trade/refund` | 交易编排、渠道适配和后台恢复 |
| `mall-admin` 下 `admin/auth`、`admin/rbac`、`admin/catalog`、`admin/trade`、`admin/report`、`admin/audit` | 管理端入口，调用所属业务门面 |
| `mall-app` 下 `app/security`、`app/config`、`app/seed` | 装配、认证过滤器和 local 种子数据 |
| `R/db/migration/V1__members.sql` 至 `V13__audit.sql` | 按以下任务顺序添加迁移；已应用的迁移不得改写 |
| `mall-app/src/test/java/com/minimall/app` | 真实 MySQL/Redis 集成测试、HTTP 权限测试和闭环测试 |
| `docs/learning`、`docs/http`、`docs/verification`、`scripts` | 中文学习说明、请求示例、验证记录、PowerShell 脚本 |

实体与 Mapper 放在所属业务包的 `persistence` 子包，类名采用 `表名语义 + Entity/Mapper`，业务接口在该业务包的 `application`，HTTP 控制器在 `api`。各任务列出必须固定的文件与契约；纯持久化映射按列明的表一对一建立，不能建立跨模块公共实体包。

数据库统一 InnoDB、utf8mb4；主键 BIGINT 自增；`created_at/updated_at` 使用 DATETIME(6) 并显式 UTC。金额、数量列加非负 CHECK。业务状态使用 VARCHAR(32) 加 Java 枚举，不使用数据库 ENUM。模块内关联采用显式外键且禁止级联删除；跨模块 ID 使用逻辑引用，由门面校验。订单历史表不对可归档商品建立级联关系。

HTTP DTO 的所有 ID 字段使用 String，应用门面内部使用 long，不能全局把所有 Long 转字符串而误伤金额。`ApiError(code,message,traceId,details)` 为统一错误结构；分页 `PageResult<T>(items,total,page,size)`；成功接口返回专用 DTO。控制器从当前身份取用户 ID，禁止信任请求体中的操作者 ID。

各任务定义的 command/view 类型放在同一 `application` 包的独立 Java record 文件中。可选 ID 使用 `Long`，时间使用 `Instant`，集合使用不可变 `List/Set`。Mapper 不作为任务间接口。凡后续任务引用的门面，必须先由所属任务定义再实现，不通过反射或字符串事件隐藏依赖。关键跨阶段契约补充：`AddressSnapshot(String receiver,String phone,String province,String city,String district,String detail,boolean defaultAddress)`；`ProductCommand(Long spuId,long categoryId,Long brandId,String title,List<String> imageUrls,List<SkuInput> skus)`；`SkuInput(Long skuId,List<Long> specificationValueIds,long priceFen)`；`ProductQuery(Long categoryId,Long brandId,String keyword,Long minPriceFen,Long maxPriceFen)`；`OrderFilter(String orderNo,Long memberId,String status,Instant from,Instant until)`。普通展示 DTO 可按所属表的非敏感字段投影，不能借此增加业务范围。

## 验证和提交的共同步骤

每项任务依次：写用例、执行预期失败（仅获授权后）、实现、执行预期通过（仅获授权后）、静态检查、提交。若无编译授权，仍可写代码与测试，但两个运行框保持未完成，记录“未执行”；不得把静态检查写成测试通过。以下核心断言是各任务测试方法的最小验收片段，变量由该任务描述的数据库、HTTP 或门面调用准备，不是新的生产接口，也不替代同任务列出的其他场景。

任务中的 `U(模块,测试类)` 和 `I(测试类)` 是以下命令的准确缩写，工作目录始终为仓库根目录：

```powershell
# U：授权编译后才执行；替换模块和测试类为任务给出的名称
.\mvnw.cmd -pl <模块> -am "-Dtest=<测试类>" "-Dsurefire.failIfNoSpecifiedTests=false" test
# I：授权编译和测试容器后才执行；Failsafe 只绑定 mall-app 的 integration profile
.\mvnw.cmd -pl mall-app -am -Pintegration "-Dit.test=<测试类>" verify
```

成功必须同时满足命令退出 0、目标测试报告存在、测试数大于 0 且 failures/errors 为 0；不能因为跳过测试而通过。失败阶段应失败在预期业务断言或尚未实现的契约，网络、Docker 不可用、依赖下载失败不算有效红灯。`I` 先执行单元测试，再通过 Failsafe 执行选定 IT；只有 mall-app 声明 Failsafe，其他模块不匹配 IT。

所有集成测试采用 Testcontainers 随机端口与独立临时数据，不连接 local 开发库；不使用 H2 模拟并发。任务 1 建立 `scripts/check-static.ps1`，不调用 Maven/Javac/Gradle/Docker build，只检查 XML、模块依赖、路径、禁用浮动版本及 `git diff --check`；退出 0 表示这些静态规则通过。

每项任务提交前运行静态脚本，检查 `git diff --stat`，只暂存本任务文件，然后使用该任务给出的中文提交说明。同一任务包含的学习说明和验证记录一并提交，禁止用 `git add .` 混入环境文件或其他工作。

## 任务 1：可装配的七模块工程与隔离开发环境

**文件：**新建根和七模块 `pom.xml`；`mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties`、`.gitignore`、`.gitattributes`、`.env.example`、`compose.yaml`、`Dockerfile`、`scripts/check-static.ps1`；`J(mall-app,app/MallApplication.java)`、`R/application.yml`、`R/application-local.yml`、`R/application-prod.yml`；`T(mall-app,app/support/IntegrationTestSupport.java)`、`T(mall-app,app/BootstrapIT.java)`。

**接口：**输入为版本表与设计模块依赖；产出唯一 `MallApplication.main(String[] args)`，测试基类统一装配 MySQL/Redis 容器和 UTC Clock；基础健康路径 `/actuator/health` 只返回状态。

- [ ] 编写 `BootstrapIT`：容器连接可用、应用上下文加载、健康状态 `UP`；编写 prod 无渠道配置启动拒绝的独立上下文用例。
  核心方法 `loadsLocalApplicationWithDependencies`：`assertEquals("UP", healthStatus); assertTrue(mysqlReachable && redisReachable);`
- [ ] 获授权后运行 `I(BootstrapIT)`，记录尚无配置时的预期失败。
- [ ] 建立父 POM 和七模块依赖；只有 app 使用 repackage；加入 Boot 对应的 WebMVC、Security、Validation、Actuator、Redis、JDBC/Flyway、测试模块，统一由父 POM 控制版本。
- [ ] Compose 服务名为 mysql/redis，项目名 mini-mall，命名数据卷；默认绑定 `127.0.0.1:13306/16379`，应用默认 18080，均可覆盖；启动前检测冲突，不停止别的服务。应用服务置于 `app` profile，Dockerfile 仅复制已打包 JAR，JRE 基础镜像在执行时固定 digest 并记录来源。
- [ ] local 显式配置模拟开关、密码来自环境变量；prod 禁用模拟和公开文档，缺渠道拒绝启动；Wrapper 下载校验和写入配置；编写静态脚本和 `docs/learning/01-工程与环境.md`。
- [ ] 获授权后运行 `I(BootstrapIT)`；静态检查并提交 `chore: 建立商城模块与隔离开发环境`。

## 任务 2：API 规范、异常映射与持久化配置

**文件：**`J(mall-common,common/api/{ApiError.java,BusinessException.java,PageQuery.java,PageResult.java,GlobalExceptionHandler.java,TraceIdFilter.java})`、`J(mall-common,common/config/{ClockConfiguration.java,MybatisConfiguration.java})`；`J(mall-app,app/config/OpenApiConfiguration.java)`；`T(mall-app,app/ApiContractIT.java)`。

**接口：**产出 `BusinessException(String code,int httpStatus,String message)`；`PageQuery(int page,int size,String sort)`；`PageResult<T>(List<T> items,long total,int page,int size)`；`Clock` bean。业务错误按 400/401/403/404/409/429/503 映射。

- [ ] 编写 `ApiContractIT`：`size=101` 返回 400；未知排序字段返回 400；ID `9007199254740993` 输出字符串，金额 12345 输出数值；错误返回 traceId 且不泄漏异常栈。
  核心方法 `rejectsOversizedPageAndPreservesId`：`assertEquals(400, oversizedPageStatus); assertEquals("9007199254740993", jsonId); assertEquals(12345L, jsonAmount);`
- [ ] 获授权后运行 `I(ApiContractIT)` 记录失败。
- [ ] 实现分页参数和统一错误；过滤器生成或校验长度受限 traceId，并在 finally 清理 MDC；Jackson 时间与 DTO ID 按上文约定；分页插件显式配置 MySQL。
- [ ] OpenAPI 按买家/管理员/回调分组，声明 Bearer 和开发密钥方案；加入 `docs/http/00-接口约定.http` 与中文错误码说明。
- [ ] 获授权后运行 `I(ApiContractIT)`；静态检查并提交 `feat: 建立接口错误分页与追踪规范`。

## 任务 3：买家账户、资料与地址

**文件：**`J(mall-common,common/auth/{ActorType.java,AccountIdentity.java,AccountDirectory.java,CurrentActor.java})`；`J(mall-user,user/account/application/MemberService.java)`、`J(mall-user,user/address/application/AddressService.java)`、对应 API 与 persistence；`R/db/migration/V1__members.sql`；`T(mall-app,app/MemberAddressIT.java)`。

**接口：**`ActorType={MEMBER,ADMIN}`；`AccountIdentity(ActorType type,long id,String passwordHash,boolean enabled,long authVersion,Set<String> permissions)`；`AccountDirectory.type():ActorType`、`findByLogin(String):Optional<AccountIdentity>`、`findById(long):Optional<AccountIdentity>`；`CurrentActor.require(ActorType):long`。

`MemberService.register(String username,String password):long`、`profile(long):MemberView`、`updateProfile(long,String nickname):void`、`changePassword(long,String oldPassword,String newPassword):void`、`setEnabled(long,boolean):void`。`AddressService.save(long memberId,Long addressId,AddressInput input):long`、`list(long):List<AddressView>`、`delete(long,long):void`、`snapshot(long,long):AddressSnapshot`；地址输入/快照含收件人、电话、省市区、详细地址和默认标记。

- [ ] 编写 `MemberAddressIT`：并发同名注册仅成功一次；甲不能读写乙地址；并发设置默认地址后最多一个默认；已建立的地址快照不受修改影响。
  核心方法 `serializesDefaultAddressUpdates`：`assertEquals(1L, defaultAddressCount); assertEquals(originalDetail, snapshot.detail());`
- [ ] 获授权后运行 `I(MemberAddressIT)` 记录失败。
- [ ] 创建 member（唯一规范化用户名、密码散列、enabled、auth_version）和 member_address；用户名限 4–32 位小写字母数字下划线，注册时规范化；密码 8–64 字符且 UTF-8 不超过 72 字节，禁止 BCrypt 静默截断。
- [ ] 实现资料与地址服务；地址更新先锁 member 行串行化默认标记；修改密码或禁用时在同一事务递增 auth_version；实现 MEMBER 账户查询适配器。HTTP 身份装配在任务 4 接入。
- [ ] 获授权后运行 `I(MemberAddressIT)`；静态检查并提交 `feat: 实现买家账户资料与收货地址`。

## 任务 4：Redis 会话认证与登录限流

**文件：**`J(mall-common,common/auth/{SessionService.java,SessionView.java})`；`J(mall-app,app/security/{RedisSessionService.java,BearerAuthenticationFilter.java,SecurityConfiguration.java,RedisLoginLimiter.java})`；`J(mall-user,user/account/api/MemberAuthController.java)`；`T(mall-app,app/SessionSecurityIT.java)`。

**接口：**消费 AccountDirectory；产出 `SessionService.login(ActorType,String login,String password,String sourceIp):SessionView`、`authenticate(String token):AccountIdentity`、`logout(String token):void`。`SessionView(String token,Instant expiresAt)`。密码校验器在 app 装配，member/admin 不依赖 app。

- [ ] 编写 `SessionSecurityIT`：有效期为 7200 秒且读取不续期；退出后 401；密码修改/账号禁用后即便 Redis 留有 token 仍拒绝；Redis 断开返回 503；不同身份令牌不能跨路由使用。
  核心方法 `revokesOldSessionDespiteRedisRetention`：`assertEquals(7200L, Duration.between(issuedAt, session.expiresAt()).toSeconds()); assertEquals(401, oldTokenStatusAfterPasswordChange); assertEquals(503, redisUnavailableStatus);`
- [ ] 获授权后运行 `I(SessionSecurityIT)` 记录失败。
- [ ] 使用 SecureRandom 32 字节令牌和 SHA-256 摘要 Redis 键，保存身份类型、ID、auth_version 与固定到期时间；每次查询数据库当前状态/版本/权限，版本不匹配删除旧会话并拒绝，解决 Redis 撤销失败窗口。
- [ ] Redis Lua 原子限流，默认账号 5 次/分钟、来源 IP 30 次/分钟；账号/IP 键使用摘要；本地不信任任意 X-Forwarded-For。注册和登录入口限流，失败回复统一；请求鉴权不建立 HttpSession，不使用 Cookie 登录，明确 CSRF 策略和关闭默认跨域放行。
- [ ] 获授权后运行 `I(SessionSecurityIT)`；补充 `docs/learning/02-认证与权限.md` 和登录 HTTP 示例；静态检查并提交 `feat: 实现Redis会话认证与登录限流`。

## 任务 5：管理员账户、角色与权限

**文件：**`J(mall-admin,admin/auth/{application/AdminAccountDirectory.java,api/AdminAuthController.java})`、`J(mall-admin,admin/rbac/application/RoleService.java)` 及 API/persistence；`J(mall-app,app/seed/LocalAdminInitializer.java)`；`R/db/migration/V2__administrators.sql`；`T(mall-app,app/AdminPermissionIT.java)`。

**接口：**ADMIN 实现 AccountDirectory；`RoleService.assign(long adminId,Set<Long> roleIds):void`、`updatePermissions(long roleId,Set<String> permissionCodes):void`、`permissions(long adminId):Set<String>`；管理员增改禁用操作进入 `AdminAccountService`，签名沿用会员服务语义，不开放匿名注册。

- [ ] 编写 `AdminPermissionIT`：运营不能改角色；客服不能改商品价格；角色权限收回后旧 token 的下次请求即拒绝；不能禁用最后一个启用的超级管理员。
  核心方法 `appliesRevokedPermissionImmediately`：`assertEquals(403, revokedPermissionStatus); assertEquals(409, disableLastSuperAdminStatus);`
- [ ] 获授权后运行 `I(AdminPermissionIT)` 记录失败。
- [ ] V2 建立五张账户/RBAC 表和唯一关联键；定义权限 `catalog:write`、`stock:adjust`、`order:read`、`shipment:write`、`aftersale:review`、`member:write`、`rbac:write`、`report:read`、`task:retry`、`audit:read`；超级管理员全量、运营商品库存发货报表、客服订单售后用户管理。
- [ ] 实现方法级权限与管理员启停/改密；LocalAdminInitializer 仅首次从环境变量读取初始密码且不打印，不覆盖已存在账户；并发权限变更锁定相关管理员行。
- [ ] 获授权后运行 `I(AdminPermissionIT)`；补充后台 HTTP 示例；静态检查并提交 `feat: 实现后台账户角色与权限`。

## 任务 6：商品目录、规格、上下架与缓存

**文件：**`J(mall-catalog,catalog/product/application/{ProductService.java,CatalogFacade.java,SkuSnapshot.java})`、对应 API/persistence、`J(mall-catalog,catalog/product/infrastructure/ProductCache.java)`；`J(mall-admin,admin/catalog/api/ProductAdminController.java)`；`R/db/migration/V3__catalog.sql`；`T(mall-app,app/ProductCatalogIT.java)`。

**接口：**`CatalogFacade.loadSaleSkus(List<Long> skuIds):List<SkuSnapshot>` 直接读取数据库并校验可售性；`lockSaleSkus(List<Long> skuIds):List<SkuSnapshot>` 要求已有事务，先按 SPU ID、再按 SKU ID 升序锁定后返回快照，供下单使用。`SkuSnapshot(long skuId,long spuId,String title,String specs,String imageUrl,long priceFen,long saleVersion)`；`ProductService.save(ProductCommand):long`、`setOnSale(long spuId,boolean):void`、`detail(long):ProductView`、`search(ProductQuery,PageQuery):PageResult<ProductView>`。分类/品牌/规格各有所属服务，管理端只调用服务。

- [ ] 编写 `ProductCatalogIT`：无 SKU 不能上架；重复规格组合拒绝；归档商品不再公开；缓存旧价时门面仍返回数据库新价；Redis 断开商品详情可读。
  核心方法 `checkoutIgnoresStalePriceCache`：`assertEquals(1200L, databaseSnapshot.priceFen()); assertEquals(200, detailStatusDuringRedisOutage);`（初始缓存价格 1000 分，数据库已改为 1200 分。）
- [ ] 获授权后运行 `I(ProductCatalogIT)` 记录失败。
- [ ] V3 建立设计列出的八张目录表；同 SPU 规格组合规范化后唯一，商品更新递增 sale_version；价格变更先锁 SPU/SKU 行，结算使用相同次序读取锁定快照，保证成交快照一致。
- [ ] 实现分类树（禁止成环）、品牌、规格和商品 CRUD/归档；标题最多 200 字符、关键词 64、URL 2048，SKU 单价上限 100000000 分；按白名单排序，LIKE 转义通配符；缓存 TTL 300–360 秒，空值 30 秒，提交后删缓存，回源不影响权威结算。
- [ ] 获授权后运行 `I(ProductCatalogIT)`；编写 `docs/learning/03-商品与库存.md` 的商品部分；静态检查并提交 `feat: 实现商品规格上下架与查询缓存`。

## 任务 7：库存占用、扣减、释放与流水

**文件：**`J(mall-catalog,catalog/stock/application/{InventoryService.java,StockLine.java,StockView.java})`、对应 persistence；`J(mall-admin,admin/catalog/api/StockAdminController.java)`；`R/db/migration/V4__inventory.sql`；`T(mall-app,app/InventoryConcurrencyIT.java)`。

**接口：**`StockLine(long skuId,int quantity)`；`InventoryService.reserve(String orderNo,List<StockLine>):void`、`commitReservation(String orderNo,List<StockLine>):void`、`release(String orderNo,List<StockLine>):void`、`restock(String refundNo,List<StockLine>):void`、`adjust(long skuId,int delta,String businessKey):void`、`get(long):StockView`；`StockView(int available,int reserved)`。前四个方法要求调用方已有事务，使用 MANDATORY，不另起事务。

- [ ] 编写 `InventoryConcurrencyIT`：可售 10，20 个并发事务各占 1，仅 10 个成功；第二 SKU 不足则第一 SKU 占用回滚；重复 release/restock 不增加库存；负数量和整数溢出拒绝。
  核心方法 `preventsOversellingUnderContention`：`assertEquals(10, successfulReservations); assertEquals(new StockView(0, 10), inventory.get(skuId));`
- [ ] 获授权后运行 `I(InventoryConcurrencyIT)` 记录失败。
- [ ] V4 建立 sku_stock 和 stock_log，库存唯一 sku_id，流水唯一 `(business_type,business_no,sku_id)`；SKU 创建同步初始化库存行；在已有事务内以 SKU ID 升序锁行，用条件更新保证非负。
- [ ] 流水同时保存变化前后值与数量；重复业务键须比对载荷，不同载荷返回冲突；管理调整仅影响可售，0 数量不写流水；补充库存阅读说明。
- [ ] 获授权后运行 `I(InventoryConcurrencyIT)`；静态检查并提交 `feat: 实现库存占用释放与幂等流水`。

## 任务 8：满减券领取与订单占用

**文件：**`J(mall-marketing,marketing/coupon/application/{CouponService.java,CouponQuote.java})`、对应 API/persistence；`J(mall-admin,admin/catalog/api/CouponAdminController.java)`；`R/db/migration/V5__coupons.sql`；`T(mall-app,app/CouponLifecycleIT.java)`。

**接口：**`CouponService.claim(long memberId,long templateId):long`、`quote(long memberId,Long couponId,long goodsFen,Instant at):CouponQuote`、`reserve(long memberId,long couponId,String orderNo,long goodsFen,Instant at):CouponQuote`、`use(String orderNo):void`、`release(String orderNo,Instant at):void`；`CouponQuote(Long couponId,long discountFen)`。reserve/use/release 要求已有事务。另提供模板创建、修改/停用及买家券列表方法。

- [ ] 编写 `CouponLifecycleIT`：满 10000 减 1000，9999 不满足，10000 满足；总量 1 并发只领取一张；同用户不能重复领；占用后模板过期仍可在订单期限内核销；过期释放为 EXPIRED；退款不返券。
  核心方法 `honorsThresholdAndExpiredRelease`：`assertEquals(1000L, eligibleQuote.discountFen()); assertEquals("EXPIRED", releasedCouponStatus); assertEquals(1L, issuedCouponCount);`
- [ ] 获授权后运行 `I(CouponLifecycleIT)` 记录失败。
- [ ] V5 创建 coupon_template、member_coupon、coupon_log；唯一 `(template_id,member_id)`；原子增加发放数量，模板行锁后处理领取；占用按券行状态条件更新。
- [ ] 模板有效期使用 `[validFrom,validUntil)`；发放后仅允许停用，金额和门槛修改通过新建模板完成；领取记录保存规则快照，避免管理员后改规则；处理无券时返回零优惠。
- [ ] 获授权后运行 `I(CouponLifecycleIT)`；补充优惠券 HTTP 示例；静态检查并提交 `feat: 实现满减券领取占用与核销`。

## 任务 9：持久化购物车与结算输入

**文件：**`J(mall-trade,trade/cart/application/CartService.java)`、对应 API/persistence；`J(mall-trade,trade/checkout/application/CheckoutLine.java)`；`R/db/migration/V6__cart.sql`；`T(mall-app,app/CartIT.java)`。

**接口：**`CheckoutLine(long skuId,int quantity)`；`CartService.add(long memberId,long skuId,int quantity):void`、`update(long memberId,long itemId,int quantity,boolean selected):void`、`remove(long memberId,long itemId):void`、`list(long memberId):List<CartItemView>`、`selected(long memberId,List<Long> itemIds):List<CheckoutLine>`、`consume(long memberId,List<CheckoutLine>):void`。consume 在订单事务中按本次购买数量扣减购物车数量，保留并发新增数量，0 时删除。

- [ ] 编写 `CartIT`：同 SKU 重复加购累加且唯一；买家不能改别人条目；下架商品列表标记失效；超 99 件拒绝；购买后保留未购买条目及剩余数量。
  核心方法 `consumesOnlyPurchasedQuantity`：`assertEquals(3, remainingQuantity); assertEquals(1L, memberSkuRowCount);`（购物车 5 件，本次购买 2 件。）
- [ ] 获授权后运行 `I(CartIT)` 记录失败。
- [ ] V6 建立 cart_item，唯一 `(member_id,sku_id)`；更新使用行锁/条件更新，新增唯一冲突后安全重读，不在已失败事务内继续；校验 1–99 数量和用户归属。
- [ ] 实现 HTTP 加改删查和勾选；购物车展示实时价格，无库存预留；结算从归属校验后的选中条目构造输入。
- [ ] 获授权后运行 `I(CartIT)`；静态检查并提交 `feat: 实现持久化购物车与选中结算`。

## 任务 10：结算试算、金额安全与价格确认

**文件：**`J(mall-trade,trade/checkout/application/{CheckoutService.java,CheckoutCommand.java,CheckoutQuote.java})`、`J(mall-trade,trade/checkout/domain/PriceCalculator.java)`、对应 API；`T(mall-trade,trade/checkout/PriceCalculatorTest.java)`、`T(mall-app,app/CheckoutIT.java)`。

**接口：**`CheckoutCommand(long addressId,List<CheckoutLine> lines,Long couponId)`；`CheckoutService.preview(long memberId,CheckoutCommand):CheckoutQuote`；`CheckoutQuote(List<SkuSnapshot> skus,List<Long> linePayableFen,AddressSnapshot address,long goodsFen,long discountFen,long freightFen,long payableFen,String fingerprint,Instant quotedAt)`。fingerprint 为排序后的 SKU、数量、saleVersion、价格、券规则、运费与地址快照的规范化摘要，无签名权限含义；提交时必须重新读取并计算。

- [ ] 编写 `PriceCalculatorTest`：`100+100+100` 分减 1 分后各行实付为 `99,100,100`；全额优惠后商品实付 0、运费仍保留；long 乘加溢出抛业务错误；重复 SKU 归并超过 99 拒绝。
  核心方法 `distributesRemainderWithoutLosingMoney`：`assertEquals(List.of(99L, 100L, 100L), result.linePayableFen()); assertEquals(299L, result.payableFen());`（此用例运费 0。）
- [ ] 获授权后运行 `U(mall-trade,PriceCalculatorTest)` 记录失败。
- [ ] 实现 `PriceCalculator.calculate(List<Long> lineGoodsFen,long discountFen,long freightFen):PriceBreakdown`；PriceBreakdown record 含各行实付及商品/优惠/运费/实付总额。乘加使用 Math exact；按比例向下取整后，按 SKU 升序分配优惠尾差；中间乘积使用 BigInteger 防溢出，再精确转 long。
- [ ] 编写并实现 CheckoutIT 对应流程：过期券、失效地址、下架 SKU 拒绝；1–50 个唯一 SKU，归并后每项 1–99；服务端金额为准，不接受客户端单价，前次 fingerprint 仅用于检测变化。
- [ ] 获授权后运行 `U(mall-trade,PriceCalculatorTest)` 和 `I(CheckoutIT)`；静态检查并提交 `feat: 实现结算试算与金额安全校验`。

## 任务 11：事务下单、快照与请求幂等

**文件：**`J(mall-trade,trade/order/application/{OrderService.java,PlaceOrderCommand.java,OrderView.java})`、`J(mall-trade,trade/order/domain/OrderStatus.java)`、对应 API/persistence；`R/db/migration/V7__orders.sql`；`T(mall-app,app/PlaceOrderIT.java)`。

**接口：**`PlaceOrderCommand(CheckoutCommand checkout,String quoteFingerprint,boolean fromCart)`；`OrderService.place(long memberId,String idempotencyKey,PlaceOrderCommand):OrderView`、`detail(long memberId,String orderNo):OrderView`、`list(long memberId,PageQuery):PageResult<OrderView>`。OrderView 至少含 orderNo、状态、金额、items、地址快照、createdAt、expiresAt、paidAt、completedAt、售后摘要；禁止返回支付秘密。

- [ ] 编写 `PlaceOrderIT`：同键同内容并发返回同一订单；同键不同内容 409；价格变化 409；多 SKU 库存不足整单回滚；地址/商品后改不影响快照；零元订单直接 PAID，库存占用归零且券 USED。
  核心方法 `deduplicatesConcurrentPlacement`：`assertEquals(1L, distinctOrderNumbers); assertEquals(409, changedPayloadStatus); assertEquals(0L, ordersAfterInsufficientStockFailure);`
- [ ] 获授权后运行 `I(PlaceOrderIT)` 记录失败。
- [ ] V7 创建 trade_order、order_item、order_address、order_status_log；唯一订单号及 `(member_id,idempotency_key)`，保存规范化请求 hash；索引 `(status,expires_at,id)` 和 `(member_id,created_at,id)`；状态日志与变更同事务。
- [ ] place 在事务内先建立订单幂等行，再依 SKU ID 锁定目录快照、库存、优惠券及购物车；重算 fingerprint，不同则回滚；保存 30 分钟截止时间与快照，调用库存/券门面。重复键异常在事务外捕获并用新事务读取原结果，避免继续使用 rollback-only 事务。
- [ ] 零元订单由相同事务调用库存确认和券核销，并在订单保存零元结算标记；非零订单等待支付。补充 `docs/learning/04-结算与订单.md`。
- [ ] 获授权后运行 `I(PlaceOrderIT)`；静态检查并提交 `feat: 实现事务下单快照与请求幂等`。

## 任务 12：持久化对接任务与租约恢复

**文件：**`J(mall-trade,trade/task/application/{IntegrationTaskService.java,TaskHandler.java,TaskLease.java,TaskResult.java})`、`J(mall-trade,trade/task/infrastructure/IntegrationTaskWorker.java)`、对应 persistence；`R/db/migration/V8__integration_tasks.sql`；`T(mall-app,app/IntegrationTaskIT.java)`。

**接口：**`IntegrationTaskService.enqueue(String type,String businessKey,String payload):long` 要求已有事务；`claim(Set<String> supportedTypes,Instant now,int limit,Duration lease):List<TaskLease>`、`complete(long taskId,String leaseToken):boolean`、`retry(long taskId,String leaseToken,String reason):boolean`、`reschedule(long taskId,String leaseToken,Instant nextRunAt):boolean`、`retryManually(long taskId):void`；`TaskHandler.type():String`、`execute(TaskLease):TaskResult`；TaskLease 含 id、type、businessKey、payload、leaseToken、leaseUntil。`TaskResult(TaskDisposition disposition,Instant nextRunAt,String reason)`，disposition 为 DONE/WAIT/RETRY，worker 分别完成、重排或累计失败重试。等待渠道 PENDING 使用 WAIT，不占用失败重试次数；等待超过可配置 24 小时转人工处理。

- [ ] 编写 `IntegrationTaskIT`：业务回滚无任务；双 worker 不重复领取有效租约；租约过期重领后旧 token 无法 complete；重试耗尽进入 MANUAL 且仍能查询；人工重试不改变 businessKey。
  核心方法 `rejectsExpiredWorkerCompletion`：`assertFalse(tasks.complete(taskId, oldLeaseToken)); assertTrue(tasks.complete(taskId, newLeaseToken));`
- [ ] 获授权后运行 `I(IntegrationTaskIT)` 记录失败。
- [ ] V8 建立 integration_task，唯一 `(type,business_key)`，索引 `(status,next_run_at,id)`；使用短事务 `FOR UPDATE SKIP LOCKED` 只领取已有处理器的类型，网络执行在事务外，写回必须匹配租约令牌和状态。WAIT 重新安排原任务，不依赖向唯一键重复插入来实现轮询；尚未实现的处理器在阶段性开发中保留待处理，最终装配测试要求所有类型都有处理器。
- [ ] 默认每 5 秒扫描 50 条、租约 60 秒、渠道超时 10 秒；失败退避 5/15/60/300/900 秒，第 6 次失败进入 MANUAL；成功终态不能重开；调度器在测试默认关闭，通过时钟和直接调用驱动测试。
- [ ] 获授权后运行 `I(IntegrationTaskIT)`；静态检查并提交 `feat: 实现持久化任务租约与失败恢复`。

## 任务 13：支付渠道契约与持久化模拟

**文件：**`J(mall-trade,trade/payment/gateway/{PaymentGateway.java,PaymentRequest.java,PaymentResult.java,RefundRequest.java,RefundResult.java,VerifiedPayment.java})`、`J(mall-trade,trade/payment/mock/{MockPaymentGateway.java,MockPaymentController.java,MockNotificationHandler.java})`；`R/db/migration/V9__payments.sql`；`T(mall-app,app/MockPaymentGatewayIT.java)`。

**接口：**`PaymentGateway.create(PaymentRequest):PaymentResult`、`query(String paymentNo):PaymentResult`、`close(String paymentNo):PaymentResult`、`refund(RefundRequest):RefundResult`、`queryRefund(String refundNo):RefundResult`、`verify(byte[] rawBody,Map<String,String> headers):VerifiedPayment`。

PaymentRequest 含 paymentNo、orderNo、amountFen、currency、expiresAt；PaymentResult 含 paymentNo、channelTransactionNo、状态（PENDING/SUCCEEDED/CLOSED/UNKNOWN）、paidAt、amountFen、currency、payParameters。RefundRequest 含 refundNo、paymentNo、amountFen；RefundResult 含 refundNo、状态（PENDING/SUCCEEDED/FAILED/UNKNOWN）、refundedAt。VerifiedPayment 含 notificationId、merchantId、paymentNo、channelTransactionNo、amountFen、currency、paidAt、status。类型字段用对应枚举，不能用一个通用 Map 代替。

- [ ] 编写 `MockPaymentGatewayIT`：同支付号重复 create 返回同一渠道单；成功后 close 返回成功事实；关闭后普通成功模拟被拒绝；错误签名/金额不接受；同退款号最多一条退款；prod 下 dev 路由不存在。
  核心方法 `keepsChannelIdempotentAndIsolated`：`assertEquals(first.channelTransactionNo(), second.channelTransactionNo()); assertEquals(1L, channelRefundCount); assertEquals(0L, registeredDevRoutesInProd);`
- [ ] 获授权后运行 `I(MockPaymentGatewayIT)` 记录失败。
- [ ] V9 建立 payment_order、payment_notification、mock_payment_transaction、mock_refund_transaction；订单唯一支付单，渠道交易号/通知号唯一；本地支付状态与模拟渠道状态各自存表，模拟渠道禁止访问订单 Mapper。
- [ ] 模拟接口显式 local/test + 开关 + 开发密钥；使用原始请求体 HMAC-SHA256 签名，固定配置商户号和 CNY；通知 payload 持久化后由任务经 HTTP 回调发送，使用固定本机回调地址，禁止客户端提供回调 URL。开发故障模式支持延迟/重复/暂时失败，以及显式异常收款注入（不混入正常付款接口）。
- [ ] 获授权后运行 `I(MockPaymentGatewayIT)`；静态检查并提交 `feat: 实现支付渠道接口与持久化模拟`。

## 任务 14：创建支付与成功通知处理

**文件：**`J(mall-trade,trade/payment/application/{PaymentService.java,PaymentNotificationService.java,PaymentOutcomeService.java})`、`J(mall-trade,trade/payment/api/{PaymentController.java,PaymentCallbackController.java})`、`J(mall-trade,trade/payment/task/CreatePaymentHandler.java)`；`T(mall-app,app/PaymentNotificationIT.java)`。

**接口：**`PaymentService.create(long memberId,String orderNo):PaymentView`、`get(long memberId,String paymentNo):PaymentView`；`PaymentNotificationService.accept(byte[] body,Map<String,String> headers):void`；`PaymentOutcomeService.apply(VerifiedPayment):void` 仅由已验证通知或可信查单调用。PaymentView 含支付号、订单号、状态及可选支付参数；创建接口先返回 202 和支付号，客户端轮询获取参数。

- [ ] 编写 `PaymentNotificationIT`：成功通知更新订单/库存/券恰好一次；不同通知 ID 重复同交易也不重复；金额/币种/商户错误不推进状态；回调先于 create 响应不会被后者覆盖；通知提交失败不返回成功应答；付费订单收款时间正好截止可接受。
  核心方法 `doesNotRegressSuccessOnLateCreateResponse`：`assertEquals("SUCCEEDED", paymentStatus); assertEquals("PAID", orderStatus); assertEquals(1L, commitReservationLogCount);`
- [ ] 获授权后运行 `I(PaymentNotificationIT)` 记录失败。
- [ ] 创建支付先锁订单、校验归属/未关闭/未过期/非零元，支付单和 CREATE_PAYMENT 任务同事务；同订单重复请求复用支付号。任务调用渠道后条件更新参数，不覆盖 SUCCEEDED/CLOSED；UNKNOWN 排队查单。
- [ ] 验签后锁订单，再锁支付记录，按库存/券顺序推进；通知唯一键处理需在安全事务边界重读；成功应答发生在事务代理返回之后。已关闭或超时收款分支记录异常收款并排队 `ENSURE_REFUND`，由任务 18 的退款执行器处理，期间任务保留待执行，不能假报退款完成。
- [ ] 获授权后运行 `I(PaymentNotificationIT)`；补充 `docs/learning/05-支付与恢复.md`；静态检查并提交 `feat: 实现支付发起与幂等回调`。

## 任务 15：取消订单、超时关闭与支付查单补偿

**文件：**`J(mall-trade,trade/order/application/OrderCloseService.java)`、`J(mall-trade,trade/order/task/OrderExpiryScanner.java)`、`J(mall-trade,trade/payment/task/{ClosePaymentHandler.java,QueryPaymentHandler.java})`；`T(mall-app,app/OrderCloseRaceIT.java)`。

**接口：**`OrderCloseService.request(long memberId,String orderNo):void`（用户取消）；`requestExpired(Instant now,int limit):int`；`finalizeClose(String orderNo,PaymentResult channelResult):void`。取消请求返回 202，后续状态从订单查询获得；无支付单时可在事务内立即释放并关闭。

- [ ] 编写 `OrderCloseRaceIT`：29:59 不关闭、30:00 扫描；期限内收款晚通知仍 PAID；期限后收款本地仍待支付则 CLOSED 并排队退款；UNKNOWN 不释放；支付成功与取消竞争无双重库存变化；已关闭成功通知不复活。
  核心方法 `refundsAfterDeadlinePaymentBeforeLocalClose`：`assertEquals("CLOSED", orderStatus); assertEquals(1L, ensureRefundTaskCount); assertEquals(initialAvailable, inventory.get(skuId).available());`
- [ ] 获授权后运行 `I(OrderCloseRaceIT)` 记录失败。
- [ ] 订单加 close_requested 标志，支付创建和关闭共享订单行锁；存在渠道单时只排队关闭/查单，不持锁调用；关闭处理中阻止新发起支付任务，已在途 create 按同 paymentNo 保持渠道一致。
- [ ] 模拟渠道 close 对不存在的支付号也创建关闭墓碑，阻止在途 create 之后复活；真实适配器契约要求同等保证或先确认 create 终态。finalizeClose 再次锁订单校验状态，成功收款走 PaymentOutcomeService；未知时间再查单，重复未知进入人工处理。
- [ ] 期限后实际收款关闭订单、释放库存与券、记录异常支付和 ENSURE_REFUND 同事务；未支付关闭才返券；超时扫描可以重复调度但唯一任务键保证不重复。统一补充锁顺序：订单→支付/售后→目录（仅下单）→库存升序→优惠券→购物车/任务。
- [ ] 获授权后运行 `I(OrderCloseRaceIT)`；静态检查并提交 `feat: 实现订单关闭与支付竞争补偿`。

## 任务 16：整单发货、模拟轨迹与确认收货

**文件：**`J(mall-trade,trade/shipping/application/ShippingService.java)`、`J(mall-trade,trade/shipping/gateway/{ShippingGateway.java,ShipmentRequest.java,ShipmentResult.java,ShipmentTrack.java})`、`J(mall-trade,trade/shipping/mock/MockShippingGateway.java)`、对应 API/task/persistence；`J(mall-admin,admin/trade/api/ShippingAdminController.java)`；`R/db/migration/V10__shipments.sql`；`T(mall-app,app/ShippingIT.java)`。

**接口：**`ShippingService.ship(String orderNo,String carrier,String trackingNo):ShipmentView`、`confirmReceipt(long memberId,String orderNo):void`、`tracks(long memberId,String orderNo):List<ShipmentTrack>`；`ShippingGateway.create(ShipmentRequest):ShipmentResult`、`query(String shipmentNo):List<ShipmentTrack>`。ShipmentRequest 含 shipmentNo/orderNo/carrier/trackingNo/地址快照，ShipmentResult 含同业务号和受理状态；轨迹含唯一事件号、eventTime、status、description。

- [ ] 编写 `ShippingIT`：待支付不可发货；重复发货同参数复用、不同参数冲突；渠道失败不新增运单；轨迹重复/乱序不倒退；仅本人可收货；有进行中售后时发货与收货被拒绝。
  核心方法 `doesNotConfirmOrderOnCarrierDelivery`：`assertEquals("SHIPPED", orderStatusAfterCarrierDelivery); assertEquals("COMPLETED", orderStatusAfterMemberConfirmation); assertEquals(1L, shipmentCount);`
- [ ] 获授权后运行 `I(ShippingIT)` 记录失败。
- [ ] V10 建立 shipment/ shipment_track 及模拟渠道独立运单表 mock_shipment、轨迹表 mock_shipment_track；订单唯一运单、事件唯一；订单保存售后占用标记供任务 17 使用。管理发货时同事务写运单、SHIPPED 和 CREATE_SHIPMENT 任务，失败在后台可见，不伪造轨迹。
- [ ] 模拟渠道按稳定 shipmentNo 幂等生成/追加轨迹；后台手动同步和定时 QUERY_SHIPMENT 共享任务；物流签收只改物流状态，确认收货才设 COMPLETED/completedAt。dev 轨迹控制沿用开发密钥与配置隔离。
- [ ] 获授权后运行 `I(ShippingIT)`；静态检查并提交 `feat: 实现整单发货物流轨迹与收货`。

## 任务 17：售后申请、审核与退货验收

**文件：**`J(mall-trade,trade/aftersale/application/AfterSaleService.java)`、`J(mall-trade,trade/aftersale/domain/AfterSaleStatus.java)`、对应 API/persistence；`J(mall-admin,admin/trade/api/AfterSaleAdminController.java)`；`R/db/migration/V11__after_sales.sql`；`T(mall-app,app/AfterSaleIT.java)`。

**接口：**`AfterSaleService.apply(long memberId,String orderNo,String reason):AfterSaleView`、`review(String afterSaleNo,boolean approved,String reason):void`、`submitReturn(long memberId,String afterSaleNo,String carrier,String trackingNo):void`、`acceptReturn(String afterSaleNo):void`、`withdraw(long memberId,String afterSaleNo):void`。类型根据订单状态确定，不由客户端强行选择。

- [ ] 编写 `AfterSaleIT`：待发货申请仅退款，已发货申请退货退款；收货第 7 天同一时刻允许、再晚 1 微秒拒绝；同订单并发申请只有一个有效单；申请与发货竞争由订单锁决定；验收进入退款中后不能撤销。
  核心方法 `enforcesSevenDayBoundaryAndSingleActiveRequest`：`assertEquals(1L, activeAfterSaleCount); assertEquals(409, applyAfterSevenDaysAndOneMicrosecondStatus); assertEquals(409, withdrawAfterRefundStartedStatus);`
- [ ] 获授权后运行 `I(AfterSaleIT)` 记录失败。
- [ ] V11 创建 after_sale/after_sale_log；订单 active_after_sale_no 作为有效售后占用，在订单锁内设置/清理；状态拒绝/撤销清理占用但保留历史；审核日志含管理员 ID 和理由。
- [ ] 审核同意仅退款、退货验收通过时，同事务设 REFUNDING、以 `AFTER_SALE:{afterSaleNo}` 作为稳定业务键排队 ENSURE_REFUND；refundNo 仅由任务 18 的 ensure 首次创建并持久化，避免两处生成冲突。acceptReturn 记录验收事实，返库统一等任务 18 确认退款成功后进行，避免退款失败时提前售出退货库存。退货未验收可撤销；退款中禁止撤销。
- [ ] 获授权后运行 `I(AfterSaleIT)`；静态检查并提交 `feat: 实现售后审核退货与验收流程`。

## 任务 18：整单退款、异常收款退款与返库

**文件：**`J(mall-trade,trade/refund/application/RefundService.java)`、`J(mall-trade,trade/refund/task/{EnsureRefundHandler.java,ExecuteRefundHandler.java,QueryRefundHandler.java})`、对应 persistence；`R/db/migration/V12__refunds.sql`；`T(mall-app,app/RefundRecoveryIT.java)`。

**接口：**`RefundService.ensure(String businessKey,String orderNo,Long afterSaleId,String reason):String`、`applyResult(String refundNo,RefundResult result):void`、`get(long memberId,String refundNo):RefundView`；业务键售后使用 `AFTER_SALE:{afterSaleNo}`，异常支付使用 `LATE_PAYMENT:{paymentNo}`。ENSURE_REFUND 的 JSON 载荷按这四个参数定义，前述任务必须一致。

- [ ] 编写 `RefundRecoveryIT`：退款含实付运费；重复通知/查单只返库一次；未验收退货不退款；零元退款不调用渠道；已关闭异常收款退款后订单仍 CLOSED 且不再次返库；调用结果未知先查单不产生第二个 refundNo；失败保留可重试状态。
  核心方法 `refundsClosedOrderWithoutRestockingTwice`：`assertEquals("CLOSED", orderStatus); assertEquals(1L, channelRefundCount); assertEquals(0L, restockLogCount);`（未支付关闭时库存已通过 RELEASE 归还。）
- [ ] 获授权后运行 `I(RefundRecoveryIT)` 记录失败。
- [ ] V12 创建 refund_order，唯一 business_key/refund_no/channel_refund_no；ensure 锁订单、校验支付金额与资格，只创建一次全额退款及 EXECUTE_REFUND 任务；重试不更换业务号。
- [ ] 渠道调用在事务外；成功后同事务更新退款、售后和订单，按未发货/已验收退货规则调用 restock；返库流水唯一 refundNo+sku，券不返还。关闭订单的异常收款只完成资金退款，不改变订单终态；退款失败或 UNKNOWN 不返库也不标成功。
- [ ] 获授权后运行 `I(RefundRecoveryIT)`，同时补跑 `I(OrderCloseRaceIT)` 的真实退款断言；编写 `docs/learning/06-履约与售后.md`；静态检查并提交 `feat: 实现整单退款与异常收款补偿`。

## 任务 19：后台查询、统计、审计与人工恢复

**文件：**`J(mall-trade,trade/order/application/TradeQueryService.java)`、`J(mall-admin,admin/trade/api/{OrderAdminController.java,TaskAdminController.java})`、`J(mall-admin,admin/report/application/DashboardService.java)`、`J(mall-admin,admin/audit/{application/AuditService.java,api/AuditController.java,infrastructure/AdminAuditInterceptor.java})`；`R/db/migration/V13__audit.sql`；`T(mall-app,app/AdminOperationsIT.java)`。

**接口：**`TradeQueryService.search(OrderFilter,PageQuery):PageResult<OrderView>`、`statistics(Instant from,Instant until):TradeStatistics`；`DashboardService.summary(LocalDate from,LocalDate until):DashboardView`；`AuditService.record(AdminAuditEvent):void`；后台任务重试消费任务 12 的 retryManually，禁止直接修改业务表状态。

- [ ] 编写 `AdminOperationsIT`：运营看得到失败发货而无权改角色；无 task:retry 不可重试；跨北京时间日期边界统计准确；日志不包含密码/token/完整地址；业务提交成功但审计失败不得谎报业务回滚。
  核心方法 `usesBeijingDayBoundaries`：`assertEquals(Instant.parse("2026-11-10T16:00:00Z"), fromInclusive); assertEquals(Instant.parse("2026-11-11T16:00:00Z"), untilExclusive);`（查询北京时间 2026-11-11 当天。）
- [ ] 获授权后运行 `I(AdminOperationsIT)` 记录失败。
- [ ] V13 建立审计表，索引操作者/时间与业务目标；管理查询走所属门面。统计口径：区间新建订单数、区间实际成功收款额、区间成功退款额、两者差额、当前待发货/待审核/人工处理任务数，排除零元渠道收款，包含异常收款及其退款，区间采用北京时间日期转 UTC 半开区间。
- [ ] 审计只记录白名单摘要；管理入口在应用服务事务返回之后记录最终结果，AuditService 使用独立事务，失败/拒绝也记录。审计入库失败写结构化告警，不能把已提交业务返回为失败，第一版不承诺审计与业务原子提交。所有管理写入口补齐权限注解和审计覆盖；人工重试保留原业务号及重试者信息。
- [ ] 获授权后运行 `I(AdminOperationsIT)`；静态检查并提交 `feat: 实现后台统计审计与人工恢复`。

## 任务 20：完整演示数据、学习文档与最终验收

**文件：**`J(mall-app,app/seed/LocalDemoInitializer.java)`；`T(mall-app,app/{MallJourneyIT.java,FailureRecoveryIT.java,ModuleBoundaryTest.java})`；`README.md`、`docs/learning/00-阅读路线.md`、`docs/http/{01-账户.http,02-商品.http,03-购物车与优惠券.http,04-订单支付.http,05-发货售后.http,06-异常恢复.http}`、`docs/verification/验收记录.md`、`scripts/{smoke.ps1,check-ports.ps1}`。

**接口：**消费前述业务门面和 HTTP API；产出可重放的完整交易演示、故障演示和实际验证记录。种子数据使用业务服务初始化，独立 local 开关与业务唯一键，不放入 prod 自动迁移，不重置已有数据。

- [ ] 编写 `MallJourneyIT`：注册→管理端上架→领券加购→下单付款→发货收货→售后退货退款，核对金额、库存、券、订单及退款终态；第二次执行不依赖第一次的数据库内容。
  核心方法 `completesPurchaseReturnAndRefundJourney`：`assertEquals("REFUNDED", orderStatus); assertEquals(paidFen, refundedFen); assertEquals(initialAvailable, finalAvailable); assertEquals("USED", couponStatus);`
- [ ] 编写 `FailureRecoveryIT` 覆盖进程级 worker 重建、重复投递、Redis 不可用和退款人工重试；`ModuleBoundaryTest` 检查模块无反向依赖且业务包不引用他模块 persistence。生产配置测试断言所有 dev 路由不存在，缺真实渠道时拒绝启动。
- [ ] 获授权后运行 `I(MallJourneyIT)`、`I(FailureRecoveryIT)` 记录发现的失败；修复对应业务文件，不把失败断言改弱。
- [ ] 编写 HTTP 文件与 smoke.ps1；脚本只访问参数指定的本地实例，不自动启动容器、不编译、不删除数据；演示账号使用唯一前缀和环境密码；故障时输出业务号便于排查，不能打印秘密。
- [ ] 完成 README 的 PowerShell 启动/停止/端口/配置/迁移/健康检查、可选容器化和故障排查；阅读路线对应 1–20 任务，说明业务问题、关键类、接口示例和取舍。写清取消请求与退款是异步的，如何轮询终态。
- [ ] 获授权后执行 `.\mvnw.cmd -Pintegration verify`，目标为所有单元/集成测试通过且无意外跳过；另获运行授权后依 README 启动应用并执行 smoke.ps1；无授权时只检查源文件与文档，并逐项保留“未执行”。
- [ ] 记录版本、命令、目标测试数、报告路径、结果和未验证事项；静态检查并提交 `docs: 完成商城演示学习资料与验收记录`。

## 阶段交付与设计覆盖自查

| 阶段 | 任务 | 可审阅成果 | 对应设计 |
| --- | --- | --- | --- |
| 基础工程 | 1–2 | 七模块、配置、接口契约、隔离测试基础 | 3–7、13 |
| 账户与权限 | 3–5 | 买家地址、会话、后台角色 | 2、7 |
| 商品与营销 | 6–10 | SPU/SKU、库存、券、购物车和金额规则 | 6、8 |
| 下单与支付 | 11–15 | 本地事务、持久任务、渠道模拟、支付关闭竞争 | 9–10、12 |
| 履约与退款 | 16–18 | 发货收货、售后、退款与恢复 | 11–12 |
| 后台与验收 | 19–20 | 后台统计审计、完整脚本和中文学习资料 | 2、13–15 |

这是一个共享交易模型的项目，用户、库存、券、支付与售后有明确顺序依赖，因此保留一份总计划，并以阶段交付；不拆成无法独立验收的多个并行子项目。

自查结论：设计要求均有归属任务；五类重点故障已纳入对应测试；跨任务契约按本文件统一；没有运行任何计划中的 Maven、容器启动或验收命令。只完成计划文档不代表代码可编译或闭环可运行。

## 执行交接

请用户审阅本计划并选择执行方式：当前会话直接逐任务实现，或按任务使用子代理实现与独立审阅。建议当前会话直接实现，因为多数任务紧密共享接口，顺序执行便于保持学习脉络，也减少反复传递上下文。选择直接实现后使用 executing-plans；选择子代理后使用 subagent-driven-development。

本计划中的运行权限与代码实施方式分开记录；用户未授权编译时继续遵守静态检查边界，不重复询问已经给出的授权。实施进度按任务更新，不把设计批准、计划批准或提交完成等同于测试通过。
