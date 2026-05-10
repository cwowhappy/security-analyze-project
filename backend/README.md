# 后端模块

基于 Gradle 9.4 + Java 21 + Spring Boot 3.5 构建。

## 技术栈

- Gradle 9.4 (Kotlin DSL)
- Java 21
- Spring Boot 3.5
- Spring JDBC (NamedParameterJdbcTemplate)
- PostgreSQL
- Flyway (数据库迁移)
- Lombok
- SLF4J + Logback

## 开发规范

### 分包规范（DDD 分层架构）

采用领域驱动设计（DDD）四层架构：

```
org.cwowhappy.securityanalyze
├── stock/                          # 业务领域
│   ├── domain/                     # 领域层 — 核心业务逻辑，无框架依赖
│   │   ├── model/                  # 领域模型（Entity、ValueObject、AggregateRoot）
│   │   ├── event/                  # 领域事件
│   │   ├── repository/             # 仓库接口（Port）
│   │   └── service/                # 领域服务
│   ├── application/                # 应用层 — 编排领域对象完成用例
│   │   ├── command/                # 写操作命令对象（CQRS Command）
│   │   ├── query/                  # 读操作查询对象（CQRS Query）
│   │   ├── dto/                    # 应用层 DTO
│   │   └── service/                # 应用服务（Use Case）
│   └── infrastructure/             # 基础设施层 — 技术实现细节
│       ├── persistence/            # 持久化实现
│       │   ├── entity/             # JDBC 数据实体
│       │   ├── mapper/             # RowMapper
│       │   └── repository/         # 仓库实现（Adapter）
│       └── config/                 # 领域级配置
├── interfaces/                     # 接口层（交付层）
│   ├── rest/
│   │   ├── controller/             # REST Controller
│   │   ├── request/                # 请求 DTO
│   │   ├── response/               # 响应 DTO + ApiResponse
│   │   └── advice/                 # 全局异常处理
│   └── filter/                     # Web 过滤器（MDC、请求日志）
├── shared/                         # 共享内核
│   ├── exception/                  # 全局业务异常
│   ├── vo/                         # 共享值对象
│   ├── event/                      # 共享事件基类
│   └── util/                       # 通用工具
└── config/                         # 全局 Spring 配置
```

#### 分层依赖规则（强制）
```
interfaces → application → domain ← infrastructure
```
- **domain**：不依赖任何外部框架（无 Spring、无 JDBC）
- **application**：可依赖 domain 和 Spring 事务，不依赖 infrastructure
- **infrastructure**：实现 domain 定义的接口（Port-Adapter 模式）
- **interfaces**：负责协议转换

### 代码规范

1. **依赖注入**：强制使用构造函数注入，禁止字段注入，利用 `@RequiredArgsConstructor`
2. **Service 设计**：应用服务只负责编排，业务规则在领域对象或领域服务中
3. **DTO 规范**：Request 使用 Jakarta Validation，Response 使用 `@Builder`
4. **统一响应**：所有 REST 接口返回 `ApiResponse<T>`
5. **Repository 规范**：领域层定义接口，基础设施层使用 `NamedParameterJdbcTemplate` 实现
6. **ID 规范**：领域对象 ID 使用强类型 Value Object（如 `StockId`），重写 `equals()` 和 `hashCode()`

### 日志规范

1. **日志框架**：SLF4J + Logback，代码中使用 `@Slf4j`
2. **日志级别**：
   - `DEBUG`：开发调试、SQL 执行
   - `INFO`：业务流程关键节点
   - `WARN`：非预期但可恢复的情况
   - `ERROR`：业务失败或系统异常（必须打印完整堆栈）
3. **MDC**：每个请求通过 `MdcFilter` 注入 `traceId`，日志格式包含 `[traceId]`
4. **日志文件**：`logs/app.log`（INFO+），`logs/error.log`（ERROR+），滚动保留 30 天

### TDD 规范

1. **TDD 循环**：严格遵守 Red → Green → Refactor
2. **测试命名**：`should{预期结果}When{条件}` 或 `{行为}_{条件}_{预期}`
3. **测试结构**：AAA 模式（Arrange - Act - Assert）
4. **测试分类**：
   - 纯单元测试：手动 new + Mockito，不启动 Spring
   - Controller 测试：`@WebMvcTest`
   - 集成测试：`@SpringBootTest` + Testcontainers
   - 架构测试：ArchUnit 验证分层依赖规则
5. **Mock 规范**：使用 `when(...).thenReturn(...)` + `verify(...)`

## 常用命令

```bash
# 构建
./gradlew build

# 运行应用
./gradlew bootRun

# 运行测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "StockAppServiceTest"
```
