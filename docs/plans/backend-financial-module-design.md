# 后端财务分析模块详细设计文档

> 版本：v1.0 | 日期：2026-05-11 | 基于 DDD 分层架构

---

## 一、模块概述

### 1.1 模块定位

财务分析模块（`financial`）是证券分析系统的核心数据层扩展，负责：
- 存储和查询上市公司财务三表（利润表、资产负债表、现金流量表）
- 计算和缓存 30+ 财务指标
- 提供趋势分析、杜邦分析、同业对比等分析能力
- 支撑 AI 财报解读的数据基础

### 1.2 设计约束

| 约束项 | 说明 |
|--------|------|
| 分层规范 | 严格遵循 `domain → application → infrastructure → interfaces` 四层 |
| 持久化技术 | Spring Data JDBC（非 JPA），手动编写 SQL |
| 主键生成 | ULID（VARCHAR(32)），与 stock/company 模块一致 |
| 数据精度 | 金额类 DECIMAL(18,2)，百分比类 DECIMAL(10,4)，比率类 DECIMAL(10,4) |
| 并发策略 | 财务数据只读为主，指标计算采用 Upsert（INSERT ON CONFLICT UPDATE）|

---

## 二、包结构与文件清单

```
backend/src/main/java/org/cwowhappy/securityanalyze/financial/
├── domain/
│   ├── model/
│   │   ├── FinancialIncome.java              # 利润表领域实体
│   │   ├── FinancialBalance.java             # 资产负债表领域实体
│   │   ├── FinancialCashflow.java            # 现金流量表领域实体
│   │   └── FinancialIndicator.java           # 财务指标领域实体
│   └── repository/
│       ├── FinancialIncomeRepository.java    # 利润表仓库接口
│       ├── FinancialBalanceRepository.java   # 资产负债表仓库接口
│       ├── FinancialCashflowRepository.java  # 现金流量表仓库接口
│       └── FinancialIndicatorRepository.java # 财务指标仓库接口
├── application/
│   ├── dto/
│   │   ├── FinancialIncomeDTO.java           # 利润表传输对象
│   │   ├── FinancialBalanceDTO.java          # 资产负债表传输对象
│   │   ├── FinancialCashflowDTO.java         # 现金流量表传输对象
│   │   ├── FinancialIndicatorDTO.java        # 财务指标传输对象
│   │   ├── TrendDataDTO.java                 # 趋势数据（图表用）
│   │   ├── DupontAnalysisDTO.java            # 杜邦分析结果
│   │   └── PeerComparisonDTO.java            # 同业对比结果
│   └── service/
│       ├── FinancialReportAppService.java    # 财报查询应用服务
│       ├── FinancialIndicatorAppService.java # 指标查询应用服务
│       └── FinancialAnalysisAppService.java  # 趋势/杜邦/对比分析服务
├── infrastructure/
│   └── persistence/
│       ├── entity/
│       │   ├── FinancialIncomeEntity.java    # 利润表 JDBC 实体
│       │   ├── FinancialBalanceEntity.java   # 资产负债表 JDBC 实体
│       │   ├── FinancialCashflowEntity.java  # 现金流量表 JDBC 实体
│       │   └── FinancialIndicatorEntity.java # 财务指标 JDBC 实体
│       ├── mapper/
│       │   ├── FinancialIncomeRowMapper.java # 利润表 RowMapper
│       │   ├── FinancialBalanceRowMapper.java# 资产负债表 RowMapper
│       │   ├── FinancialCashflowRowMapper.java# 现金流量表 RowMapper
│       │   └── FinancialIndicatorRowMapper.java# 财务指标 RowMapper
│       └── repository/
│           ├── JdbcFinancialIncomeRepository.java   # 利润表仓库实现
│           ├── JdbcFinancialBalanceRepository.java  # 资产负债表仓库实现
│           ├── JdbcFinancialCashflowRepository.java # 现金流量表仓库实现
│           └── JdbcFinancialIndicatorRepository.java# 财务指标仓库实现
└── interfaces/rest/
    └── controller/
        └── FinancialAnalysisController.java   # 财务分析 REST 控制器
```

---

## 三、领域模型设计

### 3.1 类图关系

```
┌─────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐
│   FinancialIncome   │      │  FinancialBalance   │      │  FinancialCashflow  │
├─────────────────────┤      ├─────────────────────┤      ├─────────────────────┤
│ - id: String        │      │ - id: String        │      │ - id: String        │
│ - stockCode: String │      │ - stockCode: String │      │ - stockCode: String │
│ - reportDate: Date  │      │ - reportDate: Date  │      │ - reportDate: Date  │
│ - reportType: String│      │ - reportType: String│      │ - reportType: String│
│ - basicEps: BigDec  │      │ - totalAssets: BigDec      │      │ - cfOperating: BigDec      │
│ - totalRevenue: BigDec     │      │ - totalLiabilities: BigDec │      │ - cfInvesting: BigDec      │
│ - revenue: BigDec   │      │ - totalEquity: BigDec      │      │ - cfFinancing: BigDec      │
│ - operatingCost: BigDec    │      │ - equityParentCompany: BigDec    │      │ - netCashFlow: BigDec      │
│ - grossProfit: BigDec      │      │ - currentAssets: BigDec    │      │ - freeCashFlow: BigDec     │
│ ...（共 19 字段）   │      │ ...（共 18 字段）   │      │ ...（共 10 字段）   │
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘
           │                            │                            │
           └────────────┬───────────────┴────────────┬───────────────┘
                        │                            │
                        ▼                            ▼
              ┌─────────────────────────────────────────────┐
              │          FinancialIndicator                 │
              ├─────────────────────────────────────────────┤
              │  盈利能力: roe, roa, roic, grossMargin,     │
              │           netMargin, netMarginExcl          │
              │  偿债能力: debtRatio, currentRatio,         │
              │           quickRatio, netDebtRatio,         │
              │           equityRatio                       │
              │  运营效率: dso, dio, dpo, ccc,              │
              │           assetTurnover, fixedAssetTurnover │
              │  成长性:  revenueGrowth, npParentGrowth,    │
              │           npExclGrowth, cfoGrowth,          │
              │           equityGrowth, assetGrowth         │
              │  估值:    pe, pb, ps, peg, evEbitda,        │
              │           dividendYield, marketCap          │
              │  现金流:  cfoToNp                           │
              └─────────────────────────────────────────────┘
```

### 3.2 利润表（FinancialIncome）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | ULID 主键 |
| `stockCode` | String | 股票代码，如 000001 |
| `reportDate` | LocalDate | 报告期，如 2024-12-31 |
| `reportType` | String | Y/Q1/Q2/Q3 |
| `basicEps` | BigDecimal | 基本每股收益 |
| `dilutedEps` | BigDecimal | 稀释每股收益 |
| `totalRevenue` | BigDecimal | 营业总收入 |
| `revenue` | BigDecimal | 营业收入 |
| `operatingCost` | BigDecimal | 营业成本 |
| `grossProfit` | BigDecimal | 毛利（计算字段）|
| `sellingExpense` | BigDecimal | 销售费用 |
| `adminExpense` | BigDecimal | 管理费用 |
| `rdExpense` | BigDecimal | 研发费用 |
| `financialExpense` | BigDecimal | 财务费用 |
| `operatingProfit` | BigDecimal | 营业利润 |
| `totalProfit` | BigDecimal | 利润总额 |
| `netProfit` | BigDecimal | 净利润 |
| `npParentCompany` | BigDecimal | 归母净利润 |
| `npExclNonrecurring` | BigDecimal | 扣非净利润 |

### 3.3 资产负债表（FinancialBalance）

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalAssets` | BigDecimal | 总资产 |
| `totalLiabilities` | BigDecimal | 总负债 |
| `totalEquity` | BigDecimal | 股东权益 |
| `equityParentCompany` | BigDecimal | 归母股东权益 |
| `currentAssets` | BigDecimal | 流动资产 |
| `nonCurrentAssets` | BigDecimal | 非流动资产 |
| `cashEquivalents` | BigDecimal | 货币资金 |
| `accountsReceivable` | BigDecimal | 应收账款 |
| `inventories` | BigDecimal | 存货 |
| `currentLiabilities` | BigDecimal | 流动负债 |
| `nonCurrentLiabilities` | BigDecimal | 非流动负债 |
| `accountsPayable` | BigDecimal | 应付账款 |
| `shortTermBorrowings` | BigDecimal | 短期借款 |
| `longTermBorrowings` | BigDecimal | 长期借款 |
| `goodwill` | BigDecimal | 商誉 |

### 3.4 现金流量表（FinancialCashflow）

| 字段 | 类型 | 说明 |
|------|------|------|
| `cfOperating` | BigDecimal | 经营活动现金流净额 |
| `cfInvesting` | BigDecimal | 投资活动现金流净额 |
| `cfFinancing` | BigDecimal | 筹资活动现金流净额 |
| `netCashFlow` | BigDecimal | 净现金流 |
| `freeCashFlow` | BigDecimal | 自由现金流 |
| `capex` | BigDecimal | 资本开支 |
| `cashReceivedOperating` | BigDecimal | 销售商品提供劳务收到的现金 |
| `taxPaid` | BigDecimal | 支付的各项税费 |

### 3.5 财务指标（FinancialIndicator）

| 维度 | 字段 | 类型 | 说明 |
|------|------|------|------|
| 盈利能力 | `roe` | BigDecimal | 净资产收益率 % |
| | `roa` | BigDecimal | 总资产收益率 % |
| | `roic` | BigDecimal | 投入资本回报率 % |
| | `grossMargin` | BigDecimal | 毛利率 % |
| | `netMargin` | BigDecimal | 净利率 % |
| | `netMarginExcl` | BigDecimal | 扣非净利率 % |
| 偿债能力 | `debtRatio` | BigDecimal | 资产负债率 % |
| | `currentRatio` | BigDecimal | 流动比率 |
| | `quickRatio` | BigDecimal | 速动比率 |
| | `netDebtRatio` | BigDecimal | 净负债率 % |
| | `equityRatio` | BigDecimal | 产权比率 % |
| 运营效率 | `dso` | BigDecimal | 应收账款周转天数 |
| | `dio` | BigDecimal | 存货周转天数 |
| | `dpo` | BigDecimal | 应付账款周转天数 |
| | `ccc` | BigDecimal | 现金转换周期 |
| | `assetTurnover` | BigDecimal | 总资产周转率 |
| | `fixedAssetTurnover` | BigDecimal | 固定资产周转率 |
| 成长性 | `revenueGrowth` | BigDecimal | 营收增速 % |
| | `npParentGrowth` | BigDecimal | 归母净利润增速 % |
| | `npExclGrowth` | BigDecimal | 扣非净利润增速 % |
| | `cfoGrowth` | BigDecimal | 经营现金流增速 % |
| | `equityGrowth` | BigDecimal | 净资产增速 % |
| | `assetGrowth` | BigDecimal | 总资产增速 % |
| 估值 | `pe` | BigDecimal | 市盈率 |
| | `pb` | BigDecimal | 市净率 |
| | `ps` | BigDecimal | 市销率 |
| | `peg` | BigDecimal | PEG 比率 |
| | `evEbitda` | BigDecimal | EV/EBITDA |
| | `dividendYield` | BigDecimal | 股息率 % |
| | `marketCap` | BigDecimal | 市值 |
| 现金流 | `cfoToNp` | BigDecimal | 经营现金流/净利润 % |

---

## 四、数据访问层设计

### 4.1 Repository 接口规范

所有 Repository 遵循统一接口模式：

```java
public interface FinancialXxxRepository {
    // 保存（Upsert）
    void save(FinancialXxx entity);
    
    // 批量保存
    void saveAll(List<FinancialXxx> entities);
    
    // 按股票代码查询（默认返回最近 20 期）
    List<FinancialXxx> findByStockCode(String stockCode);
    List<FinancialXxx> findByStockCode(String stockCode, String reportType);
    List<FinancialXxx> findByStockCode(String stockCode, String reportType, int limit);
    
    // 查询最近一期
    Optional<FinancialXxx> findLatest(String stockCode, String reportType);
    
    // 按报告期查询
    Optional<FinancialXxx> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType);
}
```

### 4.2 SQL 映射策略

| 操作 | SQL 模式 | 说明 |
|------|----------|------|
| 保存 | `INSERT ... ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET ...` | Upsert 语义，支持幂等写入 |
| 查询 | `SELECT * FROM tb_xxx WHERE stock_code = ? ORDER BY report_date DESC LIMIT ?` | 时间倒序 |
| 批量 | 逐条执行 Upsert（JDBC 不支持原生批量 Upsert） | 事务由 Spring @Transactional 控制 |

---

## 五、应用服务层设计

### 5.1 DTO 设计原则

- **扁平化**：DTO 直接暴露给前端，字段名采用 camelCase，与前端 TypeScript 接口对齐
- **计算字段封装**：如 `grossProfit` 在 DTO 中直接返回，无需前端计算
- **单位标注**：金额字段统一为元（前端负责转换为 万/亿）

### 5.2 服务职责划分

| 服务类 | 职责 | 依赖 |
|--------|------|------|
| `FinancialReportAppService` | 财报查询（三表） | 3 个 Repository |
| `FinancialIndicatorAppService` | 指标查询、指标计算触发 | Indicator Repository + 3 个 Repository |
| `FinancialAnalysisAppService` | 趋势分析、杜邦分析、同业对比 | 3 个 Repository + Indicator Repository |

### 5.3 核心算法：杜邦分析

```java
DupontAnalysisDTO calculateDupont(String stockCode, LocalDate reportDate) {
    // ROE = 净利率 × 资产周转率 × 权益乘数
    //     = (净利润/营收) × (营收/总资产) × (总资产/净资产)
    
    FinancialIndicator indicator = indicatorRepo.findByStockCodeAndReportDate(...)
        .orElseThrow(() -> new NotFoundException("指标不存在"));
    
    BigDecimal netMargin = indicator.getNetMargin();        // 净利率
    BigDecimal assetTurnover = indicator.getAssetTurnover(); // 资产周转率
    BigDecimal equityMultiplier = ...;                       // 权益乘数 = 总资产/净资产
    
    BigDecimal roe = netMargin.multiply(assetTurnover).multiply(equityMultiplier);
    
    return DupontAnalysisDTO.builder()
        .roe(roe)
        .netMargin(netMargin)
        .assetTurnover(assetTurnover)
        .equityMultiplier(equityMultiplier)
        .build();
}
```

### 5.4 核心算法：趋势数据

```java
TrendDataDTO getTrend(String stockCode, List<String> metrics, int periods) {
    List<FinancialIndicator> indicators = indicatorRepo.findByStockCode(stockCode, "Y", periods);
    
    // 按 metrics 参数提取对应字段，组装为图表数据
    // metrics: ["revenue", "np_parent", "roe"]
    // 返回: [{date: "2024-12-31", revenue: 1234, np_parent: 456, roe: 12.5}, ...]
}
```

---

## 六、接口层设计

### 6.1 RESTful API 清单

| 方法 | 路径 | 说明 | 查询参数 |
|------|------|------|----------|
| GET | `/api/v1/stocks/{stockCode}/financial/income` | 利润表 | `reportType`, `limit` |
| GET | `/api/v1/stocks/{stockCode}/financial/balance` | 资产负债表 | `reportType`, `limit` |
| GET | `/api/v1/stocks/{stockCode}/financial/cashflow` | 现金流量表 | `reportType`, `limit` |
| GET | `/api/v1/stocks/{stockCode}/financial/indicator` | 财务指标 | `reportType`, `limit` |
| GET | `/api/v1/stocks/{stockCode}/financial/trend` | 趋势数据 | `metrics`, `periods` |
| GET | `/api/v1/stocks/{stockCode}/financial/dupont` | 杜邦分析 | `reportDate` |
| GET | `/api/v1/stocks/{stockCode}/financial/peer-comparison` | 同业对比 | `metric` |

### 6.2 响应格式

统一使用现有 `ApiResponse<T>` 包装：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-05-11T14:30:00Z"
}
```

### 6.3 异常映射

| 异常场景 | HTTP 状态码 | 错误码 |
|----------|-------------|--------|
| 股票代码不存在 | 404 | `STOCK_NOT_FOUND` |
| 财务数据不存在 | 404 | `FINANCIAL_DATA_NOT_FOUND` |
| 指标数据不存在 | 404 | `INDICATOR_NOT_FOUND` |
| 报告期格式错误 | 400 | `INVALID_REPORT_DATE` |
| 指标参数错误 | 400 | `INVALID_METRIC` |

---

## 七、依赖关系

```
FinancialAnalysisController
    ├── FinancialReportAppService
    │   ├── FinancialIncomeRepository
    │   ├── FinancialBalanceRepository
    │   └── FinancialCashflowRepository
    ├── FinancialIndicatorAppService
    │   └── FinancialIndicatorRepository
    └── FinancialAnalysisAppService
        ├── FinancialIndicatorRepository
        ├── FinancialIncomeRepository
        ├── FinancialBalanceRepository
        └── FinancialCashflowRepository
```

> 注意：本模块**不依赖** stock/company/user 模块的领域模型，仅通过 `stockCode`（String）关联。

---

## 八、测试策略

| 测试类型 | 范围 | 工具 |
|----------|------|------|
| 单元测试 | RowMapper、DTO Builder | JUnit 5 + AssertJ |
| 集成测试 | Repository（真实 SQL）、Controller（MockMvc） | Testcontainers (PostgreSQL) |
| 架构测试 | 包依赖方向 | ArchUnit |

### 8.1 集成测试示例

```java
@Testcontainers
class JdbcFinancialIncomeRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Test
    void shouldSaveAndFindIncome() {
        FinancialIncome income = FinancialIncome.builder()
            .stockCode("000001")
            .reportDate(LocalDate.of(2024, 12, 31))
            .revenue(new BigDecimal("100000000.00"))
            .build();
        
        repository.save(income);
        
        List<FinancialIncome> results = repository.findByStockCode("000001");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRevenue()).isEqualByComparingTo("100000000.00");
    }
}
```

---

## 九、实施检查清单

- [ ] 领域模型（4 个）
- [ ] Repository 接口（4 个）
- [ ] Entity（4 个）
- [ ] RowMapper（4 个）
- [ ] JdbcRepository 实现（4 个）
- [ ] DTO（7 个）
- [ ] AppService（3 个）
- [ ] Controller（1 个）
- [ ] 全局异常处理扩展
- [ ] 集成测试（至少 4 个 Repository 测试 + 1 个 Controller 测试）
- [ ] ArchUnit 包依赖测试

---

*本文档为后端财务分析模块的开发蓝图，所有代码实现必须严格遵循本设计。*
