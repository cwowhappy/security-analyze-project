# 后端 Repository 测试覆盖率提升方案

> 本文档基于对 `backend/src/main/java/com/example/securityanalyze/**/infrastructure/` 下全部 7 个 Repository 实现类的审查结果编写，目标是为 Repository 层建立可维护、可信赖的自动化测试体系。

---

## 一、现状诊断

### 1.1 Repository 实现清单

| # | 实现类 | 对应 Domain 接口 | 复杂度 | 核心特征 |
|---|--------|----------------|--------|----------|
| 1 | `CompanyRepositoryImpl` | `CompanyRepository` | 中 | `ILIKE` 模糊查询、子查询关联 `company_security`、动态 SQL |
| 2 | `CompanySecurityRepositoryImpl` | `CompanySecurityRepository` | 中 | `ILIKE` 模糊查询、动态 SQL、日期映射 |
| 3 | `FinancialReportRepositoryImpl` | `FinancialReportRepository` | **高** | 35+ 字段映射、`jsonb` 读写、单条/批量 Upsert、日期范围查询 |
| 4 | `UserRepositoryImpl` | `UserRepository` | 中 | `ON CONFLICT ... DO UPDATE RETURNING`、枚举类型 `::user_status` / `::user_role` |
| 5 | `CollectorDashboardRepository` | 无（纯基础设施类） | 中 | CTE (`WITH`)、`DISTINCT ON`、`EXTRACT(EPOCH)`、`INTERVAL '7 days'`、JOIN |
| 6 | `IndustryRepository` | 无（纯基础设施类） | 低 | `GROUP BY`、JOIN、简单聚合 |
| 7 | `IndustryTrendAdapter` | `IndustryTrendGateway` | 低 | **非数据库类**，通过 `ProcessBuilder` 调用外部 Python 脚本 |

### 1.2 当前测试覆盖现状

- **直接测试**：`0` 个。没有任何针对 `*RepositoryImpl` 的单元测试或集成测试。
- **间接覆盖**：通过上层的 **Service 层 Mockito 测试**（Mock Repository 接口）和 **Controller 层 `@SpringBootTest` 集成测试**（部分场景真实操作数据库）间接验证。
- **问题**：手写 SQL 的正确性、RowMapper 的字段映射、PostgreSQL 专有语法的执行行为，均处于无保护状态。

### 1.3 技术约束

- **ORM 策略**：Spring Data JDBC 注解（`@Table`、`@Id`）仅用于实体声明，**所有 CRUD 均手写 SQL** + `NamedParameterJdbcTemplate`。
- **数据库强绑定**：代码深度依赖 PostgreSQL 专有特性（见下表），无法通过切换数据库规避。

| 特性 | 使用位置 | 说明 |
|------|----------|------|
| `ILIKE` | `CompanyRepositoryImpl`、`CompanySecurityRepositoryImpl` | 不区分大小写模糊匹配 |
| `::jsonb` | `FinancialReportRepositoryImpl` | `jsonb` 显式类型转换 |
| `ON CONFLICT ... DO UPDATE ... RETURNING` | `UserRepositoryImpl` | Upsert 语义 |
| `::user_status`、`::user_role` | `UserRepositoryImpl` | 自定义枚举类型转换 |
| `DISTINCT ON` | `CollectorDashboardRepository` | 按组取最新 |
| `EXTRACT(EPOCH FROM ...)` | `CollectorDashboardRepository` | 计算时间差（秒） |
| `INTERVAL '7 days'` | `CollectorDashboardRepository` | 时间区间字面量 |
| CTE (`WITH ...`) | `CollectorDashboardRepository` | 公共表表达式 |

---

## 二、测试策略：为什么必须做 Repository 层集成测试

### 2.1 为什么 Service 层的 Mockito 测试不够？

Service 层单元测试的典型写法：

```java
@Mock
private CompanyRepository companyRepository;

@Test
void shouldListCompanies() {
    when(companyRepository.findByKeyword("茅台", 0, 20))
        .thenReturn(List.of(sec1, sec2));
    // ... 只验证了业务编排逻辑，未验证 SQL 本身
}
```

**Mockito 测试只能验证"当 Repository 返回预期数据时，Service 如何处理"，无法验证：**
- SQL 语法是否在 PostgreSQL 真实环境中可执行
- `RowMapper` 是否正确处理了 `NULL` 值、`wasNull()`、日期类型、`jsonb` 解析异常分支
- 动态 SQL 的拼接逻辑（如 `keyword` 为 `null` / 空字符串时是否少拼/多拼 `WHERE`）
- 批量 Upsert 的分组逻辑是否正确
- 分页参数（`LIMIT` / `OFFSET`）是否正确绑定

### 2.2 为什么不建议 H2（兼容模式）？

常见思路是使用 H2 的 PostgreSQL 兼容模式做轻量级测试。但本项目**不适用**：

| PostgreSQL 特性 | H2 兼容模式支持情况 | 影响 |
|----------------|-------------------|------|
| `::jsonb` | 不完全支持（或语法差异） | `FinancialReportRepositoryImpl` 所有写入操作失败 |
| `ON CONFLICT ... DO UPDATE ... RETURNING` | 早期版本不支持，新版本有限支持 | `UserRepositoryImpl.save()` 核心逻辑无法测试 |
| `DISTINCT ON` | **不支持** | `CollectorDashboardRepository.findOverview()` 直接失败 |
| `::user_status`（自定义枚举类型） | 不支持 | `UserRepositoryImpl` 写入失败 |
| `ILIKE` | 支持 | 可正常工作 |
| CTE (`WITH ...`) | 有限支持 | 复杂 CTE 可能行为不一致 |

**结论**：H2 兼容模式无法覆盖本项目的核心 Repository，**必须使用真实 PostgreSQL 实例**。

### 2.3 推荐方案：`@DataJdbcTest` + Testcontainers

Spring Boot 提供了 `@DataJdbcTest` 注解，专门用于 Repository 层的**切片测试**（Slice Test）：

- 仅加载 Spring Data JDBC、JdbcTemplate、数据源相关 Bean
- 不加载 Web 层、Security、定时任务等无关组件
- 启动速度快于 `@SpringBootTest`

配合 **Testcontainers** 在测试时自动拉取并启动 PostgreSQL Docker 容器，可确保：
- 测试环境与生产环境使用**完全一致的数据库版本和语法**
- 每个测试类/方法可独立控制数据初始化和清理
- 团队成员无需本地安装 PostgreSQL 即可运行测试

---

## 三、基础设施搭建（一次性投入）

### 3.1 添加 Testcontainers 依赖

在 `backend/build.gradle` 的 `dependencies` 块中追加：

```groovy
dependencies {
    // ... 现有依赖

    // Testcontainers
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:testcontainers'
}
```

> 版本由 `spring-boot-starter-parent` / `io.spring.dependency-management` 统一管理，无需显式指定版本号。

### 3.2 创建测试专用配置

新建 `backend/src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: ${TEST_DB_URL:jdbc:tc:postgresql:16:///testdb?TC_REUSABLE=true}
    username: test
    password: test
  sql:
    init:
      mode: always
      schema-locations: classpath:db/migration/V1__create_company_table.sql
      # 如有后续迁移脚本，按顺序追加
      # data-locations: classpath:db/test-data.sql

logging:
  level:
    org.springframework.jdbc.core.JdbcTemplate: DEBUG
    org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate: DEBUG
```

关键说明：
- `jdbc:tc:postgresql:16:///testdb` 是 Testcontainers JDBC URL，Spring Boot 会自动识别并启动容器。
- `TC_REUSABLE=true` 启用容器复用（需 Testcontainers 1.19+ 和 Ryuk 支持），减少重复启动时间。
- `spring.sql.init.mode=always` 确保每次测试上下文启动时自动执行 schema 脚本。

### 3.3 创建抽象基类

新建 `backend/src/test/java/com/example/securityanalyze/common/RepositoryTestBase.java`：

```java
package com.example.securityanalyze.common;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repository 层集成测试的元注解组合。
 *
 * <p>说明：
 * <ul>
 *   <li>{@link JdbcTest} 仅加载 JDBC/数据源相关组件，不加载 Web/Security</li>
 *   <li>{@link AutoConfigureTestDatabase(replace = NONE)} 保留 application-test.yml 中配置的 Testcontainers 数据源</li>
 *   <li>{@link ComponentScan} 用于扫描 infrastructure 包下的 Repository 实现（非 Spring Data 自动生成）</li>
 *   <li>{@link ActiveProfiles("test")} 激活测试配置</li>
 *   <li>{@link Transactional} 每个测试方法后自动回滚，保证隔离性</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = "com.example.securityanalyze")
@ActiveProfiles("test")
@Transactional
public @interface RepositoryTest {
}
```

> 注：由于本项目的 Repository 是手写实现（非 Spring Data 动态代理生成），`@JdbcTest` 默认的组件扫描范围可能不包含 `@Repository` 类，因此通过 `@ComponentScan` 扩大扫描范围。如担心扫描范围过大，可精确指定 `basePackages = {"com.example.securityanalyze.company.infrastructure", ...}`。

### 3.4 可选：创建测试数据工厂

新建 `backend/src/test/java/com/example/securityanalyze/common/TestDataFactory.java`：

```java
package com.example.securityanalyze.common;

import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public final class TestDataFactory {

    private TestDataFactory() {}

    public static Company company(String unifiedCode, String name, String shortName) {
        Company c = new Company();
        c.setUnifiedCode(unifiedCode);
        c.setCompanyName(name);
        c.setShortName(shortName);
        c.setIndustry("信息技术");
        c.setRegion("北京市");
        c.setEstablishDate(LocalDate.of(2000, 1, 1));
        c.setRegisteredCapital(new BigDecimal("10000"));
        return c;
    }

    public static CompanySecurity security(Long companyId, String stockCode, String stockName) {
        CompanySecurity s = new CompanySecurity();
        s.setCompanyId(companyId);
        s.setStockCode(stockCode);
        s.setStockName(stockName);
        s.setMarket("SH");
        s.setSecurityType("A股");
        s.setListingDate(LocalDate.of(2010, 6, 1));
        s.setListingStatus("上市");
        return s;
    }

    public static FinancialReport report(String stockCode, LocalDate reportDate) {
        FinancialReport r = new FinancialReport();
        r.setStockCode(stockCode);
        r.setReportDate(reportDate);
        r.setReportType("年报");
        r.setReportYear(reportDate.getYear());
        r.setNoticeDate(reportDate.plusMonths(1));
        r.setCurrency("CNY");
        r.setTotalAssets(new BigDecimal("100000000"));
        r.setTotalRevenue(new BigDecimal("50000000"));
        r.setNetProfit(new BigDecimal("5000000"));
        r.setBalanceSheet(Map.of("key", "value"));
        return r;
    }

    public static User user(String username, UserStatus status) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("hash");
        u.setRealName("测试用户");
        u.setStatus(status);
        u.setRole(Role.USER);
        return u;
    }
}
```

---

## 四、实施路线图（按优先级排序）

### Phase 1：高价值 + 中风险（立即开始）

#### 4.1.1 `UserRepositoryImpl` — 认证核心

**优先级理由**：User 表是所有认证/授权流程的根基，且 `save()` 使用 `ON CONFLICT ... DO UPDATE ... RETURNING` 这类复杂 Upsert，一旦出错直接影响用户注册/登录。

**必测场景**：

| 场景 | 验证点 |
|------|--------|
| `save()` 插入新用户 | 返回对象有 `id`、`created_at`、`updated_at`；枚举正确映射 |
| `save()` 更新已存在用户（username 冲突） | `ON CONFLICT` 触发更新，`id` 不变，`updated_at` 刷新 |
| `findByUsername()` | 精确匹配、不存在时返回 `Optional.empty()` |
| `existsByUsername()` | 存在/不存在两种分支 |
| `findByStatus()` | 按枚举过滤，结果包含预期用户 |
| `updateStatus()` | 更新后 `findById` 验证状态变更 |

#### 4.1.2 `CompanyRepositoryImpl` — 核心业务

**优先级理由**：公司信息是系统最核心实体，`findByStockCode` 通过子查询跨表关联，模糊查询有动态 SQL 拼接逻辑。

**必测场景**：

| 场景 | 验证点 |
|------|--------|
| `findByKeyword` keyword 为 `null` | 不拼 `WHERE`，返回全量分页结果 |
| `findByKeyword` keyword 为 `"  茅台  "` | 去除首尾空格，`ILIKE` 不区分大小写匹配 |
| `findByStockCode` | 通过 `company_security` 子查询正确关联到 `company` |
| `findByStockCode` 不存在 | 返回 `Optional.empty()` |
| `findAllById` 空列表 | 返回空集合（避免拼出 `id IN ()` 语法错误） |
| `countByKeyword` | 与 `findByKeyword` 的条件逻辑保持一致 |

#### 4.1.3 `CompanySecurityRepositoryImpl`

**必测场景**：

| 场景 | 验证点 |
|------|--------|
| `findByCompanyId` | 按 `company_id` 返回多条证券记录 |
| `findByStockCode` | 精确匹配 |
| `findByKeyword` / `countByKeyword` | 同 `CompanyRepositoryImpl` 的模糊查询逻辑 |

### Phase 2：高复杂度 + 数据密集型（次优先）

#### 4.2.1 `FinancialReportRepositoryImpl` — 最复杂 Repository

**优先级理由**：35+ 字段、`jsonb` 读写、单条/批量 Upsert 混合逻辑，出错概率最高，但业务上依赖度次于 User/Company。

**必测场景**：

| 场景 | 验证点 |
|------|--------|
| `save()` 单条插入（`id == null`） | 入库成功，`jsonb` 字段可正常写入 |
| `save()` 单条更新（`id != null` 且存在） | `update` 路径触发，非 `insert` |
| `saveAll()` 混合批次（部分 insert、部分 update） | 分组逻辑正确，两条路径都执行 |
| `findByStockCode` | 返回按 `report_date DESC` 排序 |
| `findByStockCodeAndYear` | 年份过滤正确 |
| `findByStockCodeAndDateRange` | 日期区间闭区间包含边界 |
| `findByStockCodeAndReportDate` | 精确匹配 |
| `existsByStockCodeAndReportDate` | 存在/不存在 |
| `RowMapper` 处理 `NULL` 值 | `BigDecimal`、`LocalDate`、`jsonb` 均为 `NULL` 时不抛异常 |
| `RowMapper` 处理异常 `jsonb` | 脏 JSON 数据时返回 `null` 不抛异常（日志警告） |

### Phase 3：聚合查询 + 基础设施类（后续补充）

#### 4.3.1 `CollectorDashboardRepository`

**必测场景**：

| 场景 | 验证点 |
|------|--------|
| `findOverview()` | CTE + `DISTINCT ON` + `LEFT JOIN` 正确聚合；空表时不抛异常 |
| `findTasks()` 无过滤条件 | 默认 `INTERVAL '7 days'` 过滤 |
| `findTasks()` 带 `dataType` + `status` 过滤 | 动态 `AND` 拼接正确 |
| `countTasks()` | 与 `findTasks()` 条件逻辑一致 |

> 注意：该 Repository 依赖 `company`、`company_security`、`financial_report`、`collector_task_log` 四张表，测试前需插入关联数据。

#### 4.3.2 `IndustryRepository`

**必测场景**：

| 场景 | 验证点 |
|------|--------|
| `findIndustries()` | 空行业值（`NULL` / `''`）被过滤；按 `COUNT(*)` 降序 |
| `findCompaniesByIndustry()` | JOIN 正确；分页参数生效 |

#### 4.3.3 `IndustryTrendAdapter`

**说明**：该类不是数据库 Repository，而是外部进程调用（`ProcessBuilder` + Python 脚本）。

**测试策略**：单独归类为 **Gateway/Adapter 测试**，不纳入 Repository 测试体系。
- 方案 A：在 Python 侧保证 `industry_trend.py` 的单元测试（已超出 Java 后端范围）。
- 方案 B：Java 侧使用 Mockito 模拟 `ProcessBuilder`，验证命令行参数拼接和 JSON 解析异常处理。
- 方案 C：集成测试时确保 Python 环境可用，作为端到端测试的一部分（运行成本高，不建议每次构建都执行）。

**建议**：维持现状（不新增 Java 侧测试），或仅在 Java 侧补充异常分支测试（Python 进程超时、返回非 0 退出码、输出非法 JSON）。

---

## 五、示例代码

### 5.1 `UserRepositoryImplTest`

```java
package com.example.securityanalyze.user.infrastructure;

import com.example.securityanalyze.common.RepositoryTest;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@RepositoryTest
class UserRepositoryImplTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertNewUser() {
        User toSave = TestDataFactory.user("newuser", UserStatus.PENDING);

        User saved = userRepository.save(toSave);

        assertNotNull(saved.getId());
        assertEquals("newuser", saved.getUsername());
        assertEquals(UserStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldUpdateExistingUserOnConflict() {
        User first = userRepository.save(TestDataFactory.user("dup", UserStatus.PENDING));
        Long originalId = first.getId();

        User second = TestDataFactory.user("dup", UserStatus.APPROVED);
        second.setPasswordHash("new_hash");
        User updated = userRepository.save(second);

        assertEquals(originalId, updated.getId());
        assertEquals(UserStatus.APPROVED, updated.getStatus());
        assertEquals("new_hash", updated.getPasswordHash());
        assertTrue(updated.getUpdatedAt().isAfter(first.getUpdatedAt())
                || updated.getUpdatedAt().equals(first.getUpdatedAt()));
    }

    @Test
    void shouldFindByUsername() {
        userRepository.save(TestDataFactory.user("finder", UserStatus.APPROVED));

        Optional<User> found = userRepository.findByUsername("finder");

        assertTrue(found.isPresent());
        assertEquals("finder", found.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {
        Optional<User> found = userRepository.findByUsername("nobody");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldCheckExistsByUsername() {
        userRepository.save(TestDataFactory.user("exists", UserStatus.APPROVED));

        assertTrue(userRepository.existsByUsername("exists"));
        assertFalse(userRepository.existsByUsername("not_exists"));
    }

    @Test
    void shouldFindAllUsersOrderedByCreatedAtDesc() {
        userRepository.save(TestDataFactory.user("u1", UserStatus.APPROVED));
        userRepository.save(TestDataFactory.user("u2", UserStatus.PENDING));

        var all = userRepository.findAll();

        assertEquals(2, all.size());
        // 验证 ORDER BY created_at DESC：u2 在 u1 之后创建，应排在前面
        assertEquals("u2", all.get(0).getUsername());
        assertEquals("u1", all.get(1).getUsername());
    }

    @Test
    void shouldFindByStatus() {
        userRepository.save(TestDataFactory.user("approved1", UserStatus.APPROVED));
        userRepository.save(TestDataFactory.user("approved2", UserStatus.APPROVED));
        userRepository.save(TestDataFactory.user("pending1", UserStatus.PENDING));

        var approved = userRepository.findByStatus(UserStatus.APPROVED);

        assertEquals(2, approved.size());
        assertTrue(approved.stream().allMatch(u -> u.getStatus() == UserStatus.APPROVED));
    }

    @Test
    void shouldUpdateStatus() {
        User saved = userRepository.save(TestDataFactory.user("statuser", UserStatus.PENDING));

        userRepository.updateStatus(saved.getId(), UserStatus.APPROVED);

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(UserStatus.APPROVED, found.get().getStatus());
    }
}
```

### 5.2 `CompanyRepositoryImplTest`（核心场景节选）

```java
package com.example.securityanalyze.company.infrastructure;

import com.example.securityanalyze.common.RepositoryTest;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanyRepository;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@RepositoryTest
class CompanyRepositoryImplTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySecurityRepository securityRepository;

    // 注意：由于 CompanyRepositoryImpl 的 SQL 只查询 company 表，
    // 而 findByStockCode 需要关联 company_security，
    // 测试中需同时操作两张表。可通过 jdbcTemplate 直接插入基础数据，
    // 或依赖上层 Service（但 Repository 测试应保持独立）。
    // 推荐做法：使用 @Autowired NamedParameterJdbcTemplate 直接插入关联数据。

    @Test
    void shouldFindByKeywordWithNullKeyword() {
        // 直接通过底层 jdbcTemplate 插入 company 数据
        // ...

        List<Company> result = companyRepository.findByKeyword(null, 0, 10);

        assertNotNull(result);
        // 验证返回了数据且未抛异常（动态 SQL 未多拼 WHERE）
    }

    @Test
    void shouldFindByKeywordCaseInsensitive() {
        // 插入 company_name = "贵州茅台集团"
        // ...

        List<Company> result = companyRepository.findByKeyword("maotai", 0, 10);

        assertFalse(result.isEmpty());
        assertTrue(result.get(0).getCompanyName().contains("茅台"));
    }

    @Test
    void shouldFindByStockCodeViaSubQuery() {
        // 1. 插入 company 得到 id
        // 2. 插入 company_security (company_id = 上述id, stock_code = "600519")
        // 3. 调用 findByStockCode("600519")

        Optional<Company> found = companyRepository.findByStockCode("600519");

        assertTrue(found.isPresent());
    }

    @Test
    void shouldReturnEmptyForInvalidStockCode() {
        Optional<Company> found = companyRepository.findByStockCode("999999");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForEmptyIds() {
        List<Company> result = companyRepository.findAllById(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
```

### 5.3 `FinancialReportRepositoryImplTest`（批量 Upsert 场景）

```java
package com.example.securityanalyze.finance.infrastructure;

import com.example.securityanalyze.common.RepositoryTest;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.finance.domain.FinancialReport;
import com.example.securityanalyze.finance.domain.FinancialReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@RepositoryTest
class FinancialReportRepositoryImplTest {

    @Autowired
    private FinancialReportRepository repository;

    @Test
    void shouldInsertAndFindReport() {
        FinancialReport report = TestDataFactory.report("600519", LocalDate.of(2023, 12, 31));
        report.setBalanceSheet(Map.of("totalAssets", "100B"));

        repository.save(report);

        List<FinancialReport> found = repository.findByStockCode("600519");
        assertEquals(1, found.size());
        assertEquals("600519", found.get(0).getStockCode());
        assertNotNull(found.get(0).getBalanceSheet());
        assertEquals("100B", found.get(0).getBalanceSheet().get("totalAssets"));
    }

    @Test
    void shouldUpdateExistingReport() {
        FinancialReport first = TestDataFactory.report("600519", LocalDate.of(2023, 12, 31));
        repository.save(first);

        // 重新查询获取数据库生成的 id
        List<FinancialReport> existing = repository.findByStockCode("600519");
        FinancialReport toUpdate = existing.get(0);
        toUpdate.setNetProfit(new java.math.BigDecimal("99999999"));

        repository.save(toUpdate);

        Optional<FinancialReport> found = repository.findById(toUpdate.getId());
        assertTrue(found.isPresent());
        assertEquals(0, new java.math.BigDecimal("99999999").compareTo(found.get().getNetProfit()));
    }

    @Test
    void shouldBatchSaveMixedInsertAndUpdate() {
        // 先插入一条
        FinancialReport existing = TestDataFactory.report("600519", LocalDate.of(2023, 12, 31));
        repository.save(existing);
        Long existingId = repository.findByStockCode("600519").get(0).getId();

        // 准备混合批次：一条 update，一条 insert
        FinancialReport updateOne = repository.findById(existingId).orElseThrow();
        updateOne.setTotalRevenue(new java.math.BigDecimal("88888888"));

        FinancialReport insertOne = TestDataFactory.report("000001", LocalDate.of(2023, 12, 31));

        repository.saveAll(List.of(updateOne, insertOne));

        assertEquals(0, new java.math.BigDecimal("88888888")
                .compareTo(repository.findById(existingId).orElseThrow().getTotalRevenue()));
        assertFalse(repository.findByStockCode("000001").isEmpty());
    }

    @Test
    void shouldHandleNullJsonbGracefully() {
        FinancialReport report = TestDataFactory.report("600519", LocalDate.of(2023, 12, 31));
        report.setBalanceSheet(null);
        report.setProfitSheet(null);
        report.setCashFlowSheet(null);

        repository.save(report);

        List<FinancialReport> found = repository.findByStockCode("600519");
        assertEquals(1, found.size());
        assertNull(found.get(0).getBalanceSheet());
    }

    @Test
    void shouldFindByStockCodeAndYear() {
        repository.save(TestDataFactory.report("600519", LocalDate.of(2023, 12, 31)));
        repository.save(TestDataFactory.report("600519", LocalDate.of(2022, 12, 31)));

        List<FinancialReport> result = repository.findByStockCodeAndYear("600519", 2023);

        assertEquals(1, result.size());
        assertEquals(2023, result.get(0).getReportYear());
    }

    @Test
    void shouldCheckExistsByStockCodeAndReportDate() {
        LocalDate date = LocalDate.of(2023, 12, 31);
        repository.save(TestDataFactory.report("600519", date));

        assertTrue(repository.existsByStockCodeAndReportDate("600519", date));
        assertFalse(repository.existsByStockCodeAndReportDate("600519", date.plusDays(1)));
    }
}
```

---

## 六、度量与持续改进

### 6.1 Jacoco 配置优化

当前 `backend/build.gradle` 已配置 Jacoco，建议新增 Repository 层的包级覆盖阈值（渐进式收紧）：

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.30 // 项目整体最低 30%，后续逐步提升
            }
        }
        rule {
            element = 'PACKAGE'
            includes = ['com.example.securityanalyze.**.infrastructure']
            limit {
                counter = 'LINE'
                minimum = 0.60 // Repository 实现类最低 60%
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

### 6.2 团队分工建议

| 任务 | 预估工时 | 负责人建议 |
|------|----------|-----------|
| 搭建 Testcontainers 基础设施（依赖 + 基类 + 配置） | 2h | 任一后端开发 |
| `UserRepositoryImplTest` | 2h | 熟悉认证模块的同学 |
| `CompanyRepositoryImplTest` + `CompanySecurityRepositoryImplTest` | 3h | 熟悉公司模块的同学 |
| `FinancialReportRepositoryImplTest` | 4h | 熟悉财务模块的同学 |
| `CollectorDashboardRepositoryTest` | 2h | 熟悉采集模块的同学 |
| `IndustryRepositoryTest` | 1h | 熟悉行业模块的同学 |
| 接入 CI（GitHub Actions / GitLab CI 运行 Testcontainers） | 2h | DevOps 负责人 |

### 6.3 CI 注意事项

Testcontainers 在 CI 环境中运行需要 Docker 支持：

- **GitHub Actions**：官方运行器已预装 Docker，`ubuntu-latest` 直接支持。
- **GitLab CI**：需使用 `docker:dind`（Docker-in-Docker）服务，或使用具备 Docker  socket 挂载的 runner。
- **本地开发**：需安装 Docker Desktop 或 Colima（macOS）。首次运行会自动拉取 `postgres:16` 镜像，耗时约 1-2 分钟；后续通过 `TC_REUSABLE=true` 和镜像缓存可显著提速。

---

## 七、总结

| 维度 | 当前状态 | 目标状态 |
|------|----------|----------|
| Repository 直接测试 | 0 个 | 6 个（除 `IndustryTrendAdapter` 外全部覆盖） |
| 数据库真实性 | 仅 Controller 集成测试偶然使用 | 所有 Repository 测试均使用真实 PostgreSQL 16 |
| SQL 正确性验证 | 人工审查 / 运行时暴露 | 自动化测试覆盖主要查询路径和边界条件 |
| RowMapper 健壮性 | 无保护 | 验证 `NULL`、`wasNull()`、`jsonb` 异常分支 |
| 批量操作验证 | 无保护 | 验证 `saveAll()` 分组逻辑和 `batchUpdate` 正确性 |
| 团队开发体验 | 本地必须安装 PostgreSQL | 仅需 Docker，Testcontainers 自动管理数据库生命周期 |

**下一步行动**：建议先投入 2 小时搭建 Phase 1 的基础设施（Testcontainers + 基类 + `UserRepositoryImplTest` 原型），跑通第一个测试后，再按路线图分批补齐其余 Repository。
