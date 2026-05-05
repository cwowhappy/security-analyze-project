# v1.1.0__full_schema.sql 质量审查报告

**审查对象**：`backend/src/main/resources/db/release/v1.1.0__full_schema.sql`
**审查范围**：语法正确性、命名规范、类型一致性、约束完整性、索引合理性、与增量脚本及代码的匹配度
**审查日期**：2026-05-05

---

## 一、总体评价

该快照脚本结构完整，合并了 migration V1~V4 的全部对象（2 个枚举 + 16 张表 + 全部索引），语法正确，可直接用于全新环境一键初始化。与 Java Entity 和 Python Model 的字段映射基本一致，能够支撑当前业务需求。

**评分：B+**（良好，存在可优化的冗余与风格不一致）

---

## 二、中等问题（建议修正）

### 2.1 完全冗余的索引（6 处）

PostgreSQL 会在 `UNIQUE` / `PRIMARY KEY` 列上自动创建唯一索引，额外再建同列索引属于完全冗余，增加写入开销和存储空间。

| 表 | 冗余索引 | 原因 |
|----|----------|------|
| `company` | `idx_company_unified_code` | `unified_code` 已声明 `UNIQUE` |
| `company_security` | `idx_cs_stock_code` | `stock_code` 已声明 `NOT NULL UNIQUE` |
| `collector_task_log` | `idx_task_log_session_id` | `session_id` 已声明 `UNIQUE` |
| `collector_data_status` | `idx_data_status_type` | `data_type` 已声明 `NOT NULL UNIQUE` |
| `index_info` | `idx_index_code` | `index_code` 已声明 `NOT NULL UNIQUE` |
| `etf_info` | `idx_etf_code` | `etf_code` 已声明 `NOT NULL UNIQUE` |

**建议**：删除上述冗余索引。若担心某些查询计划器问题，可保留并添加注释说明。

### 2.2 复合索引前导列的单独索引（部分冗余）

以下单列/短复合索引与已有的复合唯一约束前导列重叠，在 PostgreSQL 中通常可由复合索引覆盖，属于"大概率冗余"。

| 表 | 索引 | 与哪个复合约束重叠 |
|----|------|------------------|
| `financial_report` | `idx_fin_report_stock_code` | `uk_fin_report(stock_code, report_date)` 第一列 |
| `industry_category` | `idx_industry_category_standard` | `uk(standard_code, level, code)` 第一列 |
| `industry_category` | `idx_industry_category_standard_level` | `uk(standard_code, level, code)` 前两列 |
| `company_industry_mapping` | `idx_cim_company` | `uk(company_id, standard_code, level2_code)` 第一列 |
| `company_industry_mapping` | `idx_cim_company_standard` | `uk(company_id, standard_code, level2_code)` 前两列 |
| `index_history` | `idx_ih_index_code` | `uk(index_code, trade_date, granularity)` 第一列 |
| `index_history` | `idx_ih_code_date` | `uk(index_code, trade_date, granularity)` 前两列 |
| `index_etf_mapping` | `idx_iem_index` | `uk(index_code, etf_code, relation_type)` 第一列 |

**说明**：这些索引并非绝对错误——如果查询条件总是只包含前导列而不包含后续列，PostgreSQL 确实可以使用复合索引，但保留单独索引在某些极端数据分布下可能获得更优的查询计划。建议通过 `EXPLAIN ANALYZE` 在生产数据上验证后再决定是否删除。

### 2.3 字段类型与 Java Entity 不匹配

| 表 | 字段 | SQL 类型 | Java 类型 | 风险 |
|----|------|----------|-----------|------|
| `industry_classification_standard` | `id` | `BIGSERIAL` | `Integer` | 32 位溢出风险（虽该表数据量极小，实际无影响，但风格不统一） |

### 2.4 `company_industry_mapping` UNIQUE 约束语义隐患

```sql
UNIQUE (company_id, standard_code, level2_code)
```

由于 `level2_code` 可为 `NULL`，而 PostgreSQL 的 UNIQUE 约束允许多个 `NULL` 值，这意味着：

- 同一公司、同一标准、多个 `level2_code = NULL` 的记录可以共存
- 如果业务意图是"每个标准下公司只能有一个映射"，该约束无法保证

**当前实现**：Java 代码（`CompanyIndustryMappingRepositoryImpl`）中 `is_primary` 字段用于标记主分类，业务层面通过排序 `is_primary DESC` 处理一对多。因此该设计是已知且被代码层处理的，不属于错误，但属于"约束偏弱"。

### 2.5 外键缺失（设计意图明确，建议记录）

以下字段未建立外键约束，V3 注释中标注为"预留扩展"，属于已知设计：

| 表 | 字段 | 应引用 |
|----|------|--------|
| `index_etf_mapping` | `index_code` | `index_info(index_code)` |
| `index_etf_mapping` | `etf_code` | `etf_info(etf_code)` |
| `index_history` | `index_code` | `index_info(index_code)` |
| `financial_report` | `stock_code` | `company_security(stock_code)` |

**建议**：在注释或文档中明确记录这些外键缺失的原因（如预留扩展、性能考虑、历史数据兼容性），避免后续开发者误认为是遗漏。

---

## 三、轻微问题（风格与优化建议）

### 3.1 `TIMESTAMP` 默认值风格不统一

| 表 | 风格 | 示例 |
|----|------|------|
| V1 baseline 表（company, financial_report, sys_user 等） | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` | 统一、规范 |
| V2 行业分类表（industry_category, company_industry_mapping） | `TIMESTAMP DEFAULT NOW()` | 缺少 `NOT NULL`，且使用 `NOW()` 而非 `CURRENT_TIMESTAMP` |

**建议**：统一为 `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`，保持全库风格一致。

### 3.2 `INT` / `INTEGER` 混用

- `collector_stock_sync_status.report_count` 使用 `INTEGER`
- 其他表均使用 `INT`

在 PostgreSQL 中等价，建议统一为 `INT`。

### 3.3 JSONB 字段缺少 GIN 索引

`financial_report` 表包含 3 个 JSONB 字段：`balance_sheet`、`profit_sheet`、`cash_flow_sheet`。

当前未建 GIN 索引。如果未来需要通过 JSONB 内部键值查询（如查询某特定科目的数值），缺少 GIN 索引将导致全表扫描。

**建议**：若短期内无 JSONB 查询需求，可暂不处理；若有计划，建议补充：

```sql
CREATE INDEX idx_fin_report_balance_gin ON financial_report USING GIN (balance_sheet);
```

### 3.4 全库使用 `TIMESTAMP`（无时区）

所有时间字段均为 `TIMESTAMP WITHOUT TIME ZONE`。对于金融数据系统，若未来涉及跨时区用户或海外数据源，建议逐步迁移到 `TIMESTAMPTZ`。

**当前阶段**：无实质影响，记录为长期技术债。

### 3.5 缺少数据库级 COMMENT

当前仅使用行内 `--` 注释，未使用 PostgreSQL 的 `COMMENT ON TABLE/COLUMN` 语句。这会导致数据库元数据管理工具（如 DBeaver、DataGrip、pgAdmin）无法显示表/字段的中文说明。

**建议**（低优先级）：补充：

```sql
COMMENT ON TABLE company IS '公司法人实体表';
COMMENT ON COLUMN company.unified_code IS '统一社会信用代码（预留）';
```

### 3.6 `index_history.trade_date` 单独索引利用率存疑

`idx_ih_trade_date` 单独建立在 `trade_date` 上。金融数据查询通常按"指数 + 日期范围"进行，极少单独按日期查询所有指数。该索引在多数场景下不会被使用。

**建议**：验证查询模式，如无单独按日期查全量指数的需求，可删除。

### 3.7 `industry_category.parent_code` 无外键自引用

`parent_code` 理论上应引用本表或同一标准下的行业代码，当前无约束，可能存在悬空引用。

**当前状态**：业务层（akshare 数据源）保证了数据一致性，不属于紧急问题。

---

## 四、正向评价

| 方面 | 评价 |
|------|------|
| **完整性** | 完整覆盖了 migration V1~V4 的全部对象，无遗漏表、字段或索引 |
| **与代码一致性** | Java Entity（Company、CompanySecurity、FinancialReport、IndexInfo 等）与 SQL 字段一一对应；Python Model 的 upsert SQL 与表结构匹配 |
| **幂等性** | 全量使用 `CREATE ... IF NOT EXISTS`，配合 `DROP TYPE IF EXISTS ... CASCADE`，全新环境可重复执行不报错 |
| **注释** | 每张表、每组字段均有中文注释，可读性良好 |
| **基础数据** | `industry_classification_standard` 的初始 INSERT 使用 `ON CONFLICT DO NOTHING`，幂等且安全 |
| **数值精度** | 财务数据统一使用 `DECIMAL(20,4)` 或 `DECIMAL(30,4)`，避免浮点误差；`index_history.amount` 使用 `DECIMAL(30,4)` 应对大额成交 |

---

## 五、建议修正清单（优先级排序）

| 优先级 | 事项 | 涉及文件 |
|--------|------|----------|
| P1 | 删除 6 个完全冗余索引 | `v1.1.0__full_schema.sql`、对应增量脚本 |
| P2 | 统一 V2 行业分类表的 `TIMESTAMP` 风格（`DEFAULT CURRENT_TIMESTAMP` + `NOT NULL`） | `V2__industry_classification.sql`、`v1.1.0__full_schema.sql`、`collector/sql/schema_reference.sql` |
| P3 | 统一 `INT` / `INTEGER` 用法 | 同上 |
| P4 | 补充 JSONB GIN 索引（如有 JSONB 查询需求） | `v1.1.0__full_schema.sql` |
| P5 | 补充数据库级 `COMMENT ON` 语句 | `v1.1.0__full_schema.sql` |
| P6 | 评估并清理复合索引前导列的冗余单独索引 | 需结合 `EXPLAIN ANALYZE` 验证后决策 |
| P7 | 考虑逐步迁移到 `TIMESTAMPTZ` | 长期技术债，需配合应用层修改 |

---

## 六、与增量脚本的差异核对

| 增量脚本 | 快照是否包含 | 差异说明 |
|----------|-------------|----------|
| `V1__baseline.sql` | ✅ 完整包含 | 快照增加 `IF NOT EXISTS` 和 `DROP TYPE` |
| `V2__industry_classification.sql` | ✅ 完整包含 | 快照中 `id` 已修正为 `BIGSERIAL`（增量脚本已同步修正） |
| `V3__index_module.sql` | ✅ 完整包含 | `index_info` 已合并 V4 的 `is_core` 字段 |
| `V4__index_core_flag.sql` | ✅ 结构包含 | V4 中的 `UPDATE` 初始化数据（核心指数标记）未放入快照，符合"快照只含结构"的原则 |

**结论**：快照与增量脚本的最终结构一致，合并准确。
