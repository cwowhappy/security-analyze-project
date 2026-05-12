# 数据源策略 v2

> 本文档描述 akshare 与 tushare 两个数据源的分工与采集策略。  
> 版本：v2.0 | 变更：去除实时降级逻辑与异常分层，改为顺序脚本 + 独立补充模式。

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

### 1.2 tushare

- **性质**：专业金融数据接口，Pro 版需注册 Token 和积分
- **限制**：免费用户有积分上限和调用频次限制
- **优势**：数据规范化程度高、字段定义清晰、有统一社会信用代码等官方字段
- **核心接口**：
  - `pro.stock_basic()` — 股票基础信息
  - `pro.stock_company()` — 上市公司基本信息

---

## 二、字段分工矩阵

### 2.1 股票基础信息（tb_stock_basic）

| 本表字段 | akshare 来源 | tushare 来源 | 说明 |
|----------|-------------|-------------|------|
| stock_code | `stock_info_a_code_name.code` | `stock_basic.symbol` | 两者均可 |
| ts_code | — | `stock_basic.ts_code` | 仅 tushare |
| name | `stock_info_a_code_name.name` | `stock_basic.name` | 两者均可 |
| full_name | `stock_info_sh_name_code.公司全称` | — | 仅 akshare |
| market | `stock_info_sz_name_code.板块` | `stock_basic.market` | 两者均可 |
| exchange | `stock_profile_cninfo.所属市场` | — | 仅 akshare |
| list_date | `stock_info_sh/sz_name_code.上市日期` | `stock_basic.list_date` | 两者均可 |
| industry | `stock_info_sz_name_code.所属行业` | `stock_basic.industry` | 两者均可 |
| area | — | `stock_basic.area` | 仅 tushare |
| total_shares | `stock_info_sz_name_code.A股总股本` | — | 仅 akshare |
| float_shares | `stock_info_sz_name_code.A股流通股本` | — | 仅 akshare |

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
| province | — | `stock_company.province` | 仅 tushare |
| city | — | `stock_company.city` | 仅 tushare |
| reg_address | `stock_profile_cninfo.注册地址` | — | 仅 akshare |
| office_address | `stock_profile_cninfo.办公地址` | `stock_company.office` | 两者均可 |
| website | `stock_profile_cninfo.官方网站` | `stock_company.website` | 两者均可 |
| industry | `stock_profile_cninfo.所属行业` | — | 仅 akshare |
| main_business | `stock_profile_cninfo.主营业务` | `stock_company.main_business` | 两者均可 |
| business_scope | `stock_profile_cninfo.经营范围` | `stock_company.business_scope` | 两者均可 |
| introduction | `stock_profile_cninfo.机构简介` | `stock_company.introduction` | 两者均可 |
| employees | — | `stock_company.employees` | 仅 tushare |
| controller_name | — | `stock_basic.act_name` | 仅 tushare |
| controller_type | — | `stock_basic.act_ent_type` | 仅 tushare |

---

## 三、采集策略（顺序执行）

### 3.1 股票全量采集（stock_full）

**仅使用 akshare：**
1. 调用 `stock_info_a_code_name()` 获取全量代码列表
2. 分别调用 `stock_info_sh_name_code()` 和 `stock_info_sz_name_code()` 补充详细信息
3. 合并后 upsert 到 `tb_stock_basic`
4. 单条失败记录 `fail_count`，继续下一条；失败率超过 10% 则整体标记 `failed`

### 3.2 公司全量采集（company_full）

**仅使用 akshare：**
1. 遍历 `tb_stock_basic`，逐条调用 `stock_profile_cninfo()`
2. 获取公司信息后 upsert 到 `tb_company_basic`
3. 更新 `tb_stock_basic.company_id` 建立外键关联（废弃关联表）
4. 单条失败记录 `fail_count`，继续下一条

### 3.3 字段补充采集（field_supplement）—— 独立脚本

**仅使用 tushare：**
1. 调用 `pro.stock_basic()` 补充 `area`、`ts_code`、实控人信息到 `tb_stock_basic`
2. 调用 `pro.stock_company()` 补充管理层、员工人数、统一信用代码、省份/城市到 `tb_company_basic`
3. 此脚本独立运行，失败不影响主数据完整性

### 3.4 单条精准采集

- `stock_single`：针对指定 `stock_code`，调用 akshare 个股信息接口
- `company_single`：针对指定 `stock_code`，调用 akshare `stock_profile_cninfo()`

---

## 四、容错策略（简化）

### 4.1 单条失败处理

```python
def fetch_and_save(stock_code):
    try:
        data = ak.stock_profile_cninfo(symbol=stock_code)
        db.upsert('tb_company_basic', data)
        return True
    except Exception as e:
        logger.warning(f"{stock_code} 采集失败: {e}")
        return False
```

- 单条异常仅记录日志和 `fail_count`，不阻断整体批次
- 不区分异常类型（废弃 `SourceUnavailableError` / `SourceRateLimitError` / `SourceDataError` 分层）
- 不重试（废弃重试逻辑）

### 4.2 批次失败阈值

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `BATCH_FAIL_THRESHOLD` | 批次失败率阈值 | 0.1（10%）|

当 `fail_count / total_count > BATCH_FAIL_THRESHOLD` 时：
- 中断当前批次
- 将任务标记为 `failed`
- 记录 `error_message = "批次失败率超过阈值: XX%"`
- 由人工或告警系统介入排查

---

## 五、限流控制

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `SOURCE_REQUEST_DELAY_MIN` | 请求间最小随机延迟（秒）| 1 |
| `SOURCE_REQUEST_DELAY_MAX` | 请求间最大随机延迟（秒）| 3 |

- 全量采集逐条串行执行，条间增加 `random.uniform(min, max)` 延迟
- APScheduler `max_workers=3` 控制整体并发
- 东方财富源敏感：单脚本内部不启用多线程

> 废弃 v1.0 配置：`SOURCE_MAX_RETRIES`、`SOURCE_RETRY_DELAY`、`SOURCE_RETRY_BACKOFF`。

---

## 六、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.0 | 2026-05-11 | 去除实时降级与异常分层，改为顺序脚本 + 独立补充 + 批次阈值 |
| v1.0 | 2026-05-10 | 初始版本（已废弃） |
