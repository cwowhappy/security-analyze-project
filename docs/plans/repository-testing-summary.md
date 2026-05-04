# Repository 层测试框架构建总结

## 一、整体实现思路

### 1.1 架构目标
为 Java 后端构建一套基于 **Testcontainers + PostgreSQL + Flyway** 的 Repository 集成测试框架，确保所有数据访问层代码在真实的 PostgreSQL 16 容器中运行验证，而非基于内存数据库或 Mockito Mock。

### 1.2 分层设计

```
RepositoryTestBase (抽象基类)
    ├─ @SpringBootTest(webEnvironment = NONE)   // 加载完整上下文，不启动 Web
    ├─ @Testcontainers                          // 启用 Testcontainers 生命周期管理
    ├─ @Container PostgreSQLContainer<?>         // 每个测试类启动一个 PostgreSQL 16 容器
    ├─ @DynamicPropertySource                   // 将容器 JDBC URL 注入 Spring DataSource
    ├─ @ActiveProfiles("test")                  // 激活 test profile
    └─ @Transactional                            // 每个测试方法后自动回滚

子类测试 (如 UserRepositoryImplTest)
    ├─ @Import(UserRepositoryImpl.class)        // 显式注入被测 Repository 实现
    ├─ @Autowired UserRepository                 // 注入接口调用
    ├─ @Autowired NamedParameterJdbcTemplate     // 直接操作数据库准备数据
    └─ TestDataFactory                            // 构造领域对象 & 插入辅助方法
```

### 1.3 数据准备策略
- **有 `save()` 方法的 Repository**（如 `UserRepository`、`FinancialReportRepository`）：直接调用被测 `save()` 写入数据。
- **无 `save()` 方法的 Repository**（如 `CompanyRepository`、`CompanySecurityRepository`）：通过 `TestDataFactory` 提供的 `insertXxx(jdbcTemplate, entity)` 静态 helper 直接执行 `INSERT` 并返回生成的主键。
- **需要跨表关联数据的 Repository**（如 `IndustryRepository`、`CollectorDashboardRepository`）：组合使用多个 `insertXxx` 方法，预先在关联表中写入数据。

### 1.4 Schema 初始化策略
测试环境不依赖外部数据库实例，而是通过 Flyway 在 Testcontainers 启动的空白 PostgreSQL 中自动执行 migration 脚本。为了消除历史增量脚本的兼容性负担（如 `DO $$` PL/pgSQL 块），将 V1~V6 合并为单一的 **`V1__baseline.sql`**，包含最终 schema 的全部对象。

---

## 二、遇到的关键问题及解决方案

### 问题 1：Testcontainers 无法识别 Colima 的 Docker 环境

**现象**：
```
Could not find a valid Docker environment.
EnvironmentAndSystemPropertyClientProviderStrategy: failed with exception BadRequestException
(Status 400: client version 1.32 is too old. Minimum supported API version is 1.44)
```

**根因分析**：
1. macOS 上 Colima 的 Docker socket 不在默认的 `/var/run/docker.sock`，而在 `~/.colima/default/docker.sock`。
2. Testcontainers 1.21.0 内部在 `DockerClientProviderStrategy.getClientForConfig()` 中，当 `apiVersion` 未配置时，会 hardcode fallback 到 `RemoteApiVersion.VERSION_1_32`。
3. Colima 运行的 Docker Engine 29.x 最低支持 API 1.44，因此 daemon 返回 400 Bad Request 拒绝连接。

**解决方案**：
在 `build.gradle` 的 `test` 任务中注入三项配置：
```groovy
tasks.named('test') {
    environment 'DOCKER_HOST', 'unix:///Users/lixiaoyi/.colima/default/docker.sock'
    environment 'TESTCONTAINERS_RYUK_DISABLED', 'true'
    systemProperty 'api.version', '1.53'
}
```
- `DOCKER_HOST` 指向 Colima socket。
- `TESTCONTAINERS_RYUK_DISABLED=true` 禁用 Ryuk sidecar（Ryuk 与 Colima 的 Unix socket 转发存在兼容问题）。
- `api.version=1.53` 覆盖 Testcontainers 内部的默认 1.32，确保 docker-java 客户端发送的 API 版本被 daemon 接受。

---

### 问题 2：Spring SQL init 无法解析 PostgreSQL `DO $$` 匿名代码块

**现象**：
```
PSQLException: Unterminated dollar quote started at position 3 in SQL DO $$ DECLARE old_record RECORD.
Expected terminating $$
```

**根因分析**：
- 原有 `application-test.yml` 使用 `spring.sql.init.schema-locations` 加载 migration 脚本。
- Spring 的 `ScriptUtils` 默认以 `;` 作为语句分隔符，不理解 PostgreSQL 的 dollar-quoted strings（`$$...$$`）。
- V2.1 脚本中的 `DO $$ ... END $$;` 代码块包含多个 `;`，被 Spring 错误地切割成多条不完整的语句。

**解决方案**：
引入 **Flyway** 替代 Spring SQL init：
- `build.gradle` 添加 `flyway-core` + `flyway-database-postgresql`。
- `application-test.yml` 启用 `spring.flyway.enabled=true`，禁用 `spring.sql.init.mode=never`。
- Flyway 原生支持 PostgreSQL 的所有语法特性（包括 PL/pgSQL、`DO $$`、`::jsonb` cast 等）。

---

### 问题 3：Flyway 版本冲突（archive 目录被扫描）

**现象**：
```
FlywayException: Found more than one migration with version 1
Offenders:
-> .../db/migration/archive/V1__create_company_table.sql
-> .../db/migration/V1__baseline.sql
```

**根因分析**：
Flyway 默认递归扫描 `locations` 指定的目录及其所有子目录。将旧脚本移到 `db/migration/archive/` 后，archive 子目录仍在 `classpath:db/migration` 下，导致 Flyway 同时发现两个 V1。

**解决方案**：
将归档目录移出 Flyway 扫描路径：
```
db/migration/           <-- Flyway 扫描
└── V1__baseline.sql

db/migration-archive/   <-- 脱离 Flyway 扫描
└── V1~V6 旧脚本
```

---

### 问题 4：`DataInitializer` 在测试上下文插入脏数据

**现象**：
`UserRepositoryImplTest.shouldFindAllUsersOrderedByCreatedAtDesc()` 断言 `findAll()` 返回 2 条记录，实际返回 3 条。

**根因分析**：
- `DataInitializer`（实现 `ApplicationRunner`）在 Spring 上下文启动时自动插入默认 admin 用户。
- `ApplicationRunner` 在事务外执行，数据直接提交到数据库。
- 所有 Repository 测试共享同一个 Spring 上下文缓存（因为配置相同），因此 admin 用户持续存在。

**解决方案**：
给 `DataInitializer` 加上 `@Profile("!test")`，确保测试 profile 下不执行初始化逻辑：
```java
@Component
@Profile("!test")
public class DataInitializer implements ApplicationRunner { ... }
```

---

### 问题 5：PostgreSQL ENUM 类型与 JDBC 参数类型不匹配

**现象**：
```
PSQLException: ERROR: operator does not exist: user_status = character varying
```

**根因分析**：
`UserRepositoryImpl.findByStatus()` 的 SQL 为：
```sql
SELECT * FROM sys_user WHERE status = :status ORDER BY created_at DESC
```
JDBC 将 `:status` 作为 `varchar` 传递，但 PostgreSQL 的 `user_status` 是 ENUM 类型，两者不能直接比较。

**解决方案**：
显式添加类型 cast：
```sql
SELECT * FROM sys_user WHERE status = :status::user_status ORDER BY created_at DESC
```
（`save()` 和 `updateStatus()` 方法此前已使用了 `::user_status`，但 `findByStatus()` 遗漏了。）

---

### 问题 6：`BigDecimal` 精度比较失败

**现象**：
```
AssertionFailedError: expected: <200000000> but was: <200000000.0000>
```

**根因分析**：
- 数据库字段定义为 `DECIMAL(20,4)`，PostgreSQL 返回的 `BigDecimal` 带有 4 位小数。
- Java 测试中 `new BigDecimal("200000000")` 没有小数位，`assertEquals` 使用 `equals()` 比较，精度不同导致失败。

**解决方案**：
在测试中使用 `compareTo()` 进行数值比较：
```java
assertEquals(0, new BigDecimal("200000000").compareTo(found.get().getTotalAssets()));
```

---

## 三、测试覆盖现状

| Repository | 测试类 | 方法覆盖 | 状态 |
|-----------|--------|---------|------|
| `UserRepositoryImpl` | `UserRepositoryImplTest` | 8/8 | ✅ 完整 |
| `CompanyRepositoryImpl` | `CompanyRepositoryImplTest` | 5/5 | ✅ 完整 |
| `CompanySecurityRepositoryImpl` | `CompanySecurityRepositoryImplTest` | 4/4 | ✅ 完整 |
| `FinancialReportRepositoryImpl` | `FinancialReportRepositoryImplTest` | 8/8 | ✅ 完整 |
| `IndustryRepository` | `IndustryRepositoryTest` | 3/3 | ✅ 完整 |
| `CollectorDashboardRepository` | `CollectorDashboardRepositoryTest` | 3/3 | ✅ 完整 |

**全量测试结果**：123 tests, 0 failures。

---

## 四、关键经验

1. **Testcontainers + Colima 的核心配置三要素**：`DOCKER_HOST`、`RYUK_DISABLED`、`api.version`，缺一不可。
2. **Flyway 优于 Spring SQL init**：当 migration 脚本包含 PostgreSQL 特有语法（ENUM、`DO $$`、`::jsonb`、`::类型` cast）时，Flyway 是唯一可靠的选择。
3. **数据准备与测试分离**：对于没有 `save()` 的只读 Repository，统一使用 `TestDataFactory.insertXxx()` helper，避免在每个测试类中重复编写原始 INSERT SQL。
4. **测试上下文隔离**：`@Profile("!test")` 是排除生产环境初始化逻辑侵入测试的最简洁方式。
5. **数据库类型与 Java 类型的映射陷阱**：PostgreSQL 的 `DECIMAL(p,s)` 和 ENUM 类型在 JDBC 参数绑定和结果比较时，需要特别注意 cast 和精度处理。
