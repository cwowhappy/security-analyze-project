# 数据源策略与降级设计

> 本文档描述 akshare 与 tushare 两个数据源的分工、互补逻辑与降级策略。

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
- **限制**：免费用户有积分上限和调用频次限制；高频/历史数据需付费
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
| ts_code | — | `stock_basic.ts_code` | 仅 tushare 提供 |
| name | `stock_info_a_code_name.name` | `stock_basic.name` | 两者均可 |
| full_name | `stock_info_sh_name_code.公司全称` | — | 仅 akshare |
| market | `stock_info_sz_name_code.板块` | `stock_basic.market` | akshare 更直观 |
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

## 三、采集策略

### 3.1 股票全量采集（stock_full）

1. **主源 akshare**：
   - 调用 `stock_info_a_code_name()` 获取全量代码列表
   - 分别调用 `stock_info_sh_name_code()` 和 `stock_info_sz_name_code()` 补充详细信息
   - 合并后写入 `tb_stock_basic`

2. **辅助源 tushare**（可选增强）：
   - 调用 `pro.stock_basic()` 获取 area、market、实控人信息
   - 以 `stock_code` / `ts_code` 为键，合并到已有记录

### 3.2 公司全量采集（company_full）

1. **主源 akshare**：
   - 遍历 `tb_stock_basic`，逐条调用 `stock_profile_cninfo()`
   - 获取公司全称、法人代表、注册资金、注册地址、主营业务、经营范围等
   - 写入 `tb_company_basic`

2. **辅助源 tushare**（可选增强）：
   - 调用 `pro.stock_company()` 获取管理层、员工人数、统一社会信用代码
   - 以 `stock_code` 为键，合并到已有记录

3. **关联关系建立**：
   - 采集完成后，根据 `stock_code` + `company_usc_code` 写入 `tb_relation_stock_company`

### 3.3 单条精准采集

- `stock_single`：针对指定 `stock_code`，调用 akshare 个股信息接口
- `company_single`：针对指定 `stock_code`，调用 akshare `stock_profile_cninfo()`

---

## 四、降级策略

### 4.1 异常分类与处理

```python
class DataSourceError(Exception):
    """数据源异常基类"""
    pass

class SourceUnavailableError(DataSourceError):
    """数据源不可达（网络超时、连接失败）"""
    pass

class SourceRateLimitError(DataSourceError):
    """触发限流（反爬、积分超限）"""
    pass

class SourceDataError(DataSourceError):
    """返回数据异常（格式错误、字段缺失）"""
    pass
```

### 4.2 降级流程

```
调用主数据源（akshare）
    ├── 成功 → 继续处理下一条
    ├── SourceUnavailableError / SourceRateLimitError
    │       → 延迟 3-5 秒 → 重试（最多3次）
    │       → 仍失败 → 切换备用源（tushare）
    │       → 备用源成功 → 记录日志
    │       → 备用源也失败 → fail_count + 1，跳过本条
    └── SourceDataError
            → 尝试备用源
            → 仍失败 → fail_count + 1，跳过本条
```

### 4.3 降级配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `SOURCE_MAX_RETRIES` | 单条记录重试次数 | 3 |
| `SOURCE_RETRY_DELAY` | 重试基础延迟（秒）| 3 |
| `SOURCE_RETRY_BACKOFF` | 退避倍数 | 2 |
| `SOURCE_REQUEST_DELAY_MIN` | 请求间最小延迟（秒）| 1 |
| `SOURCE_REQUEST_DELAY_MAX` | 请求间最大延迟（秒）| 3 |

---

## 五、并发与限流

- APScheduler `max_workers=5`：限制整体并发线程数
- 单条采集内部串行：全量采集时逐条处理，条间增加随机延迟
- 东方财富源敏感：避免并发请求，单线程 + 随机延迟是最佳实践
- Tushare 积分敏感：全量采集公司信息时，控制调用频率
