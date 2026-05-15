# 数据源策略 v3

> 本文档描述 akshare 与 tushare 两个数据源的分工与采集策略。  
> 版本：v3.0 | 变更：从"独立补充脚本"升级为"多数据源串行 fallback + 非空补充"模式。

---

## 一、数据源特性

### 1.1 akshare

- **性质**：开源免费，基于爬虫抓取东方财富、新浪财经等公开数据
- **限制**：东方财富源有反爬机制，需控制请求频率；接口可能因网站改版失效
- **优势**：完全免费、无积分限制、A股股票列表和个股信息接口稳定
- **核心接口**：
  - `stock_info_a_code_name()` — A股全量代码和名称
  - `stock_info_sh_name_code()` / `stock_info_sz_name_code()` — 上交所/深交所详细信息
  - `stock_profile_cninfo()` — 巨潮资讯公司详情（字段最全）
  - `stock_financial_report_sina()` — 新浪财经三表数据

### 1.2 tushare

- **性质**：专业金融数据接口，Pro 版需注册 Token 和积分
- **限制**：免费用户有积分上限和调用频次限制
- **优势**：数据规范化程度高、字段定义清晰、有统一社会信用代码等官方字段
- **核心接口**：
  - `pro.stock_basic()` — 股票基础信息（批量返回全市场）
  - `pro.stock_company()` — 上市公司基本信息（per-stock）

---

## 二、字段分工矩阵

### 2.1 股票基础信息（tb_stock_basic）

| 本表字段 | akshare 来源 | tushare 来源 | 说明 |
|----------|-------------|-------------|------|
| stock_code | `stock_info_a_code_name.code` | `stock_basic.symbol` | 两者均可 |
| ts_code | — | `stock_basic.ts_code` | 仅 tushare |
| name | `stock_info_a_code_name.name` | `stock_basic.name` | 两者均可 |
| full_name | `stock_info_sh_name_code.公司全称` | `stock_basic.fullname` | 两者均可 |
| market | `stock_info_sz_name_code.板块` | `stock_basic.market` | 两者均可 |
| exchange | 代码前缀推断 | `stock_basic.exchange` | 两者均可 |
| list_date | `stock_info_sh/sz_name_code.上市日期` | `stock_basic.list_date` | 两者均可 |
| industry | `stock_info_sz_name_code.所属行业` | `stock_basic.industry` | 两者均可 |
| area | — | `stock_basic.area` | 仅 tushare |
| total_shares | `stock_info_sz_name_code.A股总股本` | `daily_basic.total_share` ×10000 | 两者均可 |
| float_shares | `stock_info_sz_name_code.A股流通股本` | `daily_basic.float_share` ×10000 | 两者均可 |

### 2.2 公司基础信息（tb_company_basic）

| 本表字段 | akshare 来源 | tushare 来源 | 说明 |
|----------|-------------|-------------|------|
| unified_social_credit_code | — | `stock_company.com_id` | 仅 tushare |
| name | `stock_profile_cninfo.公司名称` | `stock_company.com_name` | 两者均可 |
| short_name | `stock_profile_cninfo.公司简称` | — | 仅 akshare |
| english_name | `stock_profile_cninfo.英文名称` | — | 仅 akshare |
| former_name | `stock_profile_cninfo.曾用简称` | — | 仅 akshare |
| legal_representative | `stock_profile_cninfo.法人代表` | — | 仅 akshare |
| chairman | — | `stock_company.chairman` | 仅 tushare |
| manager | — | `stock_company.manager` | 仅 tushare |
| secretary | — | `stock_company.secretary` | 仅 tushare |
| reg_capital | `stock_profile_cninfo.注册资金` | `stock_company.reg_capital` | 两者均可 |
| setup_date | `stock_profile_cninfo.成立日期` | `stock_company.setup_date` | 两者均可 |
| province | 注册地址解析 | `stock_company.province` | 两者均可 |
| city | 注册地址解析 | `stock_company.city` | 两者均可 |
| reg_address | `stock_profile_cninfo.注册地址` | — | 仅 akshare |
| office_address | `stock_profile_cninfo.办公地址` | `stock_company.office` | 两者均可 |
| website | `stock_profile_cninfo.官方网站` | `stock_company.website` | 两者均可 |
| industry | `stock_profile_cninfo.所属行业` | — | 仅 akshare |
| main_business | `stock_profile_cninfo.主营业务` | `stock_company.main_business` | 两者均可 |
| business_scope | `stock_profile_cninfo.经营范围` | `stock_company.business_scope` | 两者均可 |
| introduction | `stock_profile_cninfo.机构简介` | `stock_company.introduction` | 两者均可 |
| employees | — | `stock_company.employees` | 仅 tushare |

---

## 三、采集策略（多源串行 fallback）

### 3.1 核心原则

- **主源优先**：akshare 作为默认主源（priority=1），tushare 作为补充源（priority=2）
- **非空不覆盖**：主源已成功返回的字段，补充源不会覆盖
- **自动降级**：主源失败时自动尝试补充源，无需人工干预
- **字段互补**：利用多数据源互补特性，最大化字段覆盖率

### 3.2 配置示例

```yaml
# stock_basic.yaml
task_type: stock_basic
ttl_hours: 24
sources:
  - name: akshare
    adapter: stock_basic_akshare_adapter
    priority: 1
    field_mapping:
      - { api_field: "代码", db_field: "stock_code", converter: "str", null_policy: "fail" }
      - { api_field: "名称", db_field: "name", converter: "str", null_policy: "fail" }
      # ...
  - name: tushare
    adapter: stock_basic_tushare_adapter
    priority: 2
    field_mapping:
      - { api_field: "ts_code", db_field: "ts_code", converter: "str", null_policy: "skip" }
      - { api_field: "industry", db_field: "industry", converter: "str", null_policy: "skip" }
      - { api_field: "area", db_field: "area", converter: "str", null_policy: "skip" }
      # ...
```

### 3.3 执行时行为

```
1. 尝试 akshare：
   - 成功：获取 stock_code, name, market, exchange 等基础字段
   - 失败：记录日志，继续下一步

2. 尝试 tushare：
   - 成功：获取 ts_code, industry, area, total_shares 等字段
   - 若 akshare 已返回 industry，则 tushare 的 industry 被忽略（非空不覆盖）
   - 若 akshare 未返回 area，则 tushare 的 area 被补充
   - 失败：记录日志

3. 合并后的记录持久化到 tb_stock_basic
```

### 3.4 单条精准采集

`single` 模式同样遵循多源 fallback 逻辑：
- 针对指定 `stock_code`，按 `source_priority` 顺序尝试各数据源
- 无视 TTL，强制重新采集

---

## 四、容错策略

### 4.1 自适应调速与重试

由 `AdaptiveRequestEngine` 自动管理：

| 错误类型 | 处理方式 | 是否重试 |
|----------|----------|----------|
| 429 / Timeout / 503 | 指数退避：`delay = min(delay * 2 + jitter, max_delay)` | 是（最多 3 次） |
| 404 / 数据不存在 | `NonRecoverableError`，不重试 | 否 |
| 字段转换异常 | 当前 source 失败，fallback 到下一源 | 否（直接 fallback） |
| `null_policy=fail` | 当前 source 失败，fallback 到下一源 | 否 |

### 4.2 批次失败阈值

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `BATCH_FAIL_THRESHOLD` | 批次失败率阈值 | 0.1（10%）|

当某 `batch_size`（默认 20 只）的失败率超过阈值时：
- 中断当前 Task，标记为 `FAILED`
- 已处理的数据和 `tb_collection_stock_state` 状态永久保留
- 未处理的股票保持 `pending`，下次任务重启时自动恢复

### 4.3 断点恢复

`full` 模式支持精确断点恢复：
- 每只股票的处理状态独立持久化到 `tb_collection_stock_state`
- 重启任务时自动跳过 `success` 且未超过 `ttl_hours`（默认 24 小时）的记录
- 失败/过期记录自动重新采集

---

## 五、限流控制

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `ADAPTIVE_MIN_DELAY` | 最小调用间隔（秒）| 1.0 |
| `ADAPTIVE_MAX_DELAY` | 最大调用间隔（秒）| 60.0 |
| `ADAPTIVE_BACKOFF_JITTER` | 退避抖动范围（秒）| 0.5 |
| `ADAPTIVE_SUCCESS_THRESHOLD` | 连续成功多少次后尝试降速 | 10 |

- 每个数据源独立维护 delay 状态
- 长时间平稳运行时自动尝试降低间隔，动态收敛到最优频率
- 遇到限流信号后自动增大间隔，避免触发封禁

---

## 六、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| **v3.0** | **2026-05-13** | **多数据源串行 fallback + 非空补充；字段映射外置到 YAML；自适应调速替代固定延迟；断点恢复支持 TTL** |
| v2.0 | 2026-05-11 | 去除实时降级与异常分层，改为顺序脚本 + 独立补充 + 批次阈值 |
| v1.0 | 2026-05-10 | 初始版本（已废弃） |
