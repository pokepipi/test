# 校园二手物品交易平台（Spring Boot + Vue 前后端分离）

一个功能完整的校园二手物品交易平台毕业设计项目，覆盖 **物品发布 / 分类检索 / 私信议价 / 订单全流程 / 多角色权限 / 数据可视化 / 图片压缩 / 违规内容审核** 等核心功能，前后端分离架构，开箱即用。

## 一、功能清单

| 模块 | 功能 |
|---|---|
| 用户认证 | 注册 / 登录（JWT 令牌）、BCrypt 密码加密、个人资料与头像、修改密码 |
| 多角色权限 | 学生（STUDENT）/ 管理员（ADMIN）双角色，后端拦截器 + 前端路由双重控制 |
| 物品管理 | 发布（自动敏感词扫描 → 待审核）、两级分类、关键字/分类/价格区间检索、排序（最新/价格/热度）、浏览量统计、上/下架、编辑、删除 |
| 违规内容审核 | DFA 敏感词过滤器（自研）、发布内容自动扫描、管理员人工审核（通过/驳回）、敏感词词典管理 + 在线检测 |
| 私信议价 | 基于 WebSocket 的实时聊天、按物品维度会话分组、未读消息、敏感词拦截 |
| 订单管理 | 下单（支持议价后价格）→ 模拟支付 → 卖家发货 → 买家确认收货 → 完成；买家未付款可取消、卖家发货前可取消并恢复物品 |
| 交易数据可视化 | 管理后台 ECharts 仪表盘：核心指标卡、7 日成交趋势、分类成交分布、热销 TOP10、订单状态分布、用户增长 |
| 图片处理 | 上传自动压缩（>1200px 等比缩放 + 质量压缩），GIF 动图原样保留 |
| 管理端 | 物品审核、用户禁用/启用、物品/订单全量管理、分类管理、敏感词管理 |
| 缓存优化 | 物品详情 Redis 缓存（空值防穿透 / 随机 TTL 防雪崩 / 写操作主动失效）；浏览量 Redis 累加 + 定时批量回写，避免每次浏览都写库；Redis 不可用时自动降级为直连数据库 |
| 分库分表 | 基于 ShardingSphere-JDBC 把订单表按 buyer_id 取模拆成 orders_0~3，通过独立 profile 启用，默认不影响原有运行方式 |

## 二、技术栈

- **后端**：Spring Boot 2.7 / MyBatis-Plus 3.5 / MySQL 8 / **Redis（物品详情缓存 + 浏览量计数）** / **ShardingSphere-JDBC 5.3（订单分库分表）** / JWT (jjwt) / Spring WebSocket / Thumbnailator 图片压缩 / spring-security-crypto(BCrypt)
- **前端**：Vue 3 (Composition API) / Vite 5 / Element Plus / Pinia / Vue Router / Axios / ECharts 5
- **架构**：前后端分离，RESTful API，统一响应 `Result{code,message,data}`，JWT 无状态认证

## 三、目录结构

```
campus-trading/
├── sql/schema.sql              # 数据库脚本（6 张表 + 种子数据）
├── sql/schema-sharding.sql     # 分库分表脚本（orders_0~3，仅启用分片时需要）
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/campus/trading/
│       ├── common/             # 统一响应 / 业务异常 / 全局异常处理 / 缓存 key 常量
│       ├── config/             # JWT拦截器 / CORS / WebSocket / MyBatis-Plus / Redis 序列化
│       ├── controller/         # REST 接口
│       ├── dto/                # 入参对象（JSR-303 校验）
│       ├── entity/             # 实体
│       ├── init/               # 启动初始化（默认账号/词典）
│       ├── mapper/             # MyBatis-Plus Mapper
│       ├── service/            # 业务逻辑（含 CacheService：缓存统一入口 + 降级保护）
│       ├── task/               # 定时任务（浏览量从 Redis 批量回写 MySQL）
│       └── util/               # JWT / DFA敏感词 / 图片压缩
├── frontend/                   # Vue3 前端
│   └── src/
│       ├── api/index.js        # 全部接口封装
│       ├── router/index.js     # 路由 + 权限守卫
│       ├── store/user.js       # Pinia 用户状态
│       ├── utils/              # axios 封装 / 格式化
│       ├── components/         # 物品卡片 / 图片上传
│       └── views/              # 用户端 + admin/ 管理端页面
└── docs/论文大纲.md             # 毕业论文撰写大纲
```

## 四、环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.x
- Node.js 16+（推荐 18+）

## 五、快速启动

### 1. 初始化数据库

```powershell
mysql -u root -p < sql/schema.sql
```

> 默认数据库 `campus_trading`，账号密码默认 `root/123456`，可用环境变量 `DB_PASSWORD` 覆盖。

### 2. 启动后端（端口 8080）

```powershell
cd backend
mvn spring-boot:run
# 或打包运行: mvn clean package -DskipTests && java -jar target/campus-trading-backend-1.0.0.jar
```

启动成功会看到日志：`初始化管理员账号: admin / admin123`、`初始化演示学生账号: student / 123456`。

> **关于 Redis：不装也能跑。** 物品详情缓存和浏览量计数会走 Redis，若本机没装 Redis，`CacheService` 会打印一条
> `[cache] Redis 不可用, 已降级为直接访问数据库` 的告警，然后自动回退成原来的「每次查库 + 直接写库」逻辑，
> 服务照常启动、功能不受影响。想体验缓存效果再装（Windows 可用 Memurai 或 Docker：
> `docker run -d -p 6379:6379 redis:7`），装好后无需改配置，重启即生效。
>
> **本机已经装好了**（Redis 5.0.14.1，绿色版）：目录 `..\redis`，双击 `start-redis.bat` 即可启动，
> 端口 6379、无密码，与 `application.yml` 的默认配置一致。开机后如果发现又走了降级分支，先跑一下这个 bat。

### 3. 启动前端（端口 5173）

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问 **http://localhost:5173**（开发环境已配置代理转发 `/api`、`/uploads`、`/ws` 到 8080）。

### 4. （可选）启用分库分表

订单表按 `buyer_id % 4` 拆成 `orders_0 ~ orders_3`，由 ShardingSphere-JDBC 负责路由。默认不启用，需要时按下面三步走：

```powershell
# ① 建分表并迁移存量数据
mysql -u root -p --default-character-set=utf8mb4 -e "source sql/schema-sharding.sql"

# ② 用 sharding profile 启动后端(驱动会被换成 ShardingSphereDriver)
mvn spring-boot:run -Dspring-boot.run.profiles=sharding
# 或: java -jar target/campus-trading-backend-1.0.0.jar --spring.profiles.active=sharding
```

③ 访问「我的订单」接口，控制台会打印改写后的真实 SQL（`sql-show: true`），能看到逻辑表 `orders` 被路由到具体某张分表：

```
Logic SQL: SELECT ... FROM orders WHERE buyer_id = ?
Actual SQL: ds0 ::: SELECT ... FROM orders_1 WHERE buyer_id = ?
```

**实现要点（面试常问）：**

- **为什么用驱动方式接入**：ShardingSphere 5.3.0 起不再提供 Spring Boot Starter，改为把自身实现成 JDBC 驱动，
  只需替换 `driver-class-name` 和 `url`（`jdbc:shardingsphere:classpath:sharding.yaml`），
  业务代码、MyBatis-Plus 完全不用改，也不用担心自动配置冲突。
- **分片键怎么选**：选 `buyer_id`，因为「我的订单」是最主要的访问路径，带分片键能精准路由到单表；
  按 `seller_id` 或 `id` 查询时会广播到 4 张表再归并，功能正确但更慢——这也是分片键选择的典型取舍。
- **主键怎么保证全局唯一**：`orders` 用的是 MySQL 自增主键，拆表后各表都会从 1 开始自增必然撞车。
  这里用「分段起点」（各表自增起点错开 100 万）解决，不需要改代码；
  数据量真正大了应该换雪花算法，但要额外处理前端 JS 对 19 位 Long 的精度丢失问题。
- **存量数据迁移**：按同样的取模规则 `INSERT INTO orders_N SELECT ... WHERE MOD(buyer_id,4)=N` 打散，
  脚本末尾会校验分表总数是否与原表一致。

**踩过的一个真实坑（面试可以讲）：**

接入后启动直接失败，报 `NoSuchMethodError: 'void ...ShardingSphereYamlConstructor$1.setCodePointLimit(int)'`。
根因是 **SnakeYAML 版本冲突**：ShardingSphere 5.3.2 解析 YAML 需要 snakeyaml ≥ 1.32（`setCodePointLimit` 是 1.32 引入的），
而 Spring Boot 2.7 的依赖管理把 snakeyaml 钉在 1.30 —— **dependencyManagement 优先级高于传递依赖**，
所以 ShardingSphere 自己声明的 1.33 被覆盖掉了，最终打进 jar 的是 1.30。

解决：在 pom 里显式覆盖 `<snakeyaml.version>1.33</snakeyaml.version>`。
**为什么选 1.33 而不是 2.x**：1.33 与 1.30 同属 1.x 线，Spring Boot 自身解析 `application.yml` 不受影响；
直接跳到 2.x 有把 Boot 的配置加载搞挂的风险。

排查手段：`mvn dependency:tree -Dincludes=org.yaml:snakeyaml` 看实际生效版本，
再解开 fat jar（`BOOT-INF/lib/snakeyaml-*.jar`）确认最终打进去的是哪个版本。

## 六、默认账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | admin | admin123 |
| 学生 | student | 123456 |

## 七、演示流程（建议按此走一遍）

1. **学生登录**（student/123456）→ 首页浏览/搜索物品
2. 发布一件物品（注意：发布后为**待审核**，首页不可见）
3. **管理员登录**（admin/admin123）→ 管理后台「物品审核」→ 通过/驳回
4. 回到前台，刷新首页可见已上架物品 → 点进详情 → **联系卖家**私信议价 → 谈拢后**立即购买**（可填议价后的价格）
5. 「我的订单」：支付（模拟）→ 卖家（用另一个学生账号或本账号发起的物品）发货 → 买家确认收货
6. 管理员 → 数据统计：查看各类图表
7. 试试发布含敏感词（如「代考」）的物品：自动命中敏感词标记；管理员可查看命中提示并驳回

## 八、核心接口一览

```
# 认证
POST /api/auth/register        POST /api/auth/login        GET  /api/auth/me
# 物品
GET  /api/item/page            公开分页检索(关键字/分类/价格/排序)
GET  /api/item/{id}            详情(浏览量+1)
POST /api/item                 发布(自动敏感词扫描)
PUT  /api/item/{id}            编辑(重新审核)
PUT  /api/item/{id}/status     上/下架
GET  /api/item/my              我的物品
# 聊天 (WebSocket 实时推送 /ws/chat?token=xxx)
GET  /api/chat/conversations   GET /api/chat/messages
POST /api/chat/send            GET /api/chat/unread
# 订单 (0待付款 1待发货 2待收货 3已完成 4已取消)
POST /api/order/create
POST /api/order/{id}/pay|cancel|ship|finish
# 管理端 (需 ADMIN, 前缀 /api/admin/**)
GET  /api/admin/stats/overview|trend|category|top-items|status|user-growth
GET  /api/admin/audit/page     POST /api/admin/audit/{id}
GET  /api/admin/user/page      PUT  /api/admin/user/{id}/status
GET  /api/admin/item/page      PUT  /api/admin/item/{id}/status
GET  /api/admin/order/page
POST /api/admin/category       PUT/DELETE /api/admin/category/{id}
GET  /api/admin/sensitive-word/list|check
POST /api/admin/sensitive-word DELETE /api/admin/sensitive-word/{id}
```

## 九、常见问题

- **数据库连不上**：修改 `backend/src/main/resources/application.yml` 中 datasource 的账号密码，或设置环境变量 `DB_PASSWORD`。
- **端口被占用**：8080 被占改 `server.port`；5173 被占改 `vite.config.js` 的 `server.port`。
- **图片 404**：确认后端工作目录下有 `uploads/` 目录生成（`mvn spring-boot:run` 启动时在后端目录下）。
- **生产部署**：前端 `npm run build` 产物放 nginx，配置 `/api`、`/uploads`、`/ws` 反代到后端即可；`/api/admin/**` 由后端拦截器保证仅管理员可访问。
- **JDK 版本**：兼容 JDK 8 ~ 23。项目已内置两项 JDK 兼容处理：Lombok 升级到 1.18.34，并在 maven-compiler-plugin 显式声明注解处理器路径（JDK 23 起 javac 不再自动从 classpath 发现处理器）；已包含 jaxb-api 依赖（JDK 9+ 使用 jjwt 所需）。
