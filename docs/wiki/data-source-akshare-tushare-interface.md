# 数据源接口手册：AKShare & Tushare

> 本文档整理 AKShare 与 Tushare 在股票基本信息、公司基本信息与财报数据方面的主要获取接口及返回字段，供证券分析系统数据采集模块（collector）参考选型。
> 整理日期：2026-05-13

---

## 目录

- [一、AKShare](#一akshare)
  - [1.1 股票基本信息](#11-股票基本信息)
  - [1.2 公司基本信息](#12-公司基本信息)
  - [1.3 财务报表数据](#13-财务报表数据)
  - [1.4 财务分析指标](#14-财务分析指标)
  - [1.5 业绩报表与快报](#15-业绩报表与快报)
- [二、Tushare Pro](#二tushare-pro)
  - [2.1 股票基本信息](#21-股票基本信息)
  - [2.2 公司基本信息](#22-公司基本信息)
  - [2.3 财务报表数据](#23-财务报表数据)
  - [2.4 财务指标数据](#24-财务指标数据)
  - [2.5 业绩预告](#25-业绩预告)
- [三、接口对比与采集建议](#三接口对比与采集建议)

---

## 一、AKShare

AKShare 是基于 Python 的开源金融数据接口库，主要数据源为东方财富、同花顺、新浪财经、腾讯证券等公开数据。

### 1.1 股票基本信息

#### 1.1.1 全市场股票列表

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_info_a_code_name` | A 股代码和名称对照表 | code（代码）、name（名称） |
| `stock_info_sh_name_code` | 沪 A 股代码和名称 | 公司代码、公司简称、代码、简称、所属行业 |
| `stock_info_sz_name_code` | 深 A 股代码和名称 | A股代码、A股简称、所属行业、总市值 |
| `stock_info_bj_name_code` | 北交所股票代码和名称 | 代码、名称 |

#### 1.1.2 个股基本信息（单只查询）

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `stock_individual_info_em` | 东方财富-个股信息 | `symbol`: 股票代码如 `"000001"` | item/value 形式：最新价、股票代码、股票简称、总股本、流通股、总市值、流通市值、行业、上市时间 |
| `stock_individual_basic_info_xq` | 雪球-公司概况 | `symbol`: 如 `"SH600519"` | item/value 形式：org_id、org_name_cn（公司全称）、org_short_name_cn、main_operation_business（主营业务）、operating_scope（经营范围）、legal_representative（法定代表人）、general_manager（总经理）、secretary（董秘）、established_date（成立日期）、reg_asset（注册资本）、staff_num（员工人数）、actual_controller（实际控制人）、classi_name（企业性质）、chairman（董事长）、issue_price（发行价）、actual_issue_vol（实际发行量）、pe_after_issuing（发行市盈率）、affiliate_industry（所属行业） |
| `stock_profile_cninfo` | 巨潮资讯-公司概况 | `symbol`: 如 `"600519"` | 标准列式 DataFrame：公司名称、英文名称、曾用简称、A股代码、A股简称、B股代码、H股代码、入选指数、所属市场、所属行业、法人代表、注册资金、成立日期、上市日期、官方网站、电子邮箱、联系电话、传真、注册地址、办公地址、邮政编码、主营业务、经营范围、机构简介 |

#### 1.1.3 实时行情（含基本信息）

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_zh_a_spot_em` | 沪深京 A 股实时行情 | 序号、代码、名称、最新价、涨跌幅、涨跌额、成交量（手）、成交额（元）、振幅、最高、最低、今开、昨收、量比、换手率、市盈率-动态、市净率、总市值、流通市值、涨速、5分钟涨跌、60日涨跌幅、年初至今涨跌幅 |
| `stock_sh_a_spot_em` | 沪 A 股实时行情 | 同上 |
| `stock_sz_a_spot_em` | 深 A 股实时行情 | 同上 |
| `stock_bj_a_spot_em` | 京 A 股（北交所）实时行情 | 同上 |
| `stock_cy_a_spot_em` | 创业板实时行情 | 同上 |
| `stock_kc_a_spot_em` | 科创板实时行情 | 同上 |
| `stock_new_a_spot_em` | 新股实时行情 | 同上 + 上市时间 |

> 说明：实时行情接口中包含 `总市值`、`流通市值`、`市盈率-动态`、`市净率` 等估值指标，可作为股票基本信息的补充。

---

### 1.2 公司基本信息

#### 1.2.1 公司概况与主营

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `stock_zyjs_ths` | 同花顺-主营介绍 | `symbol`: 股票代码 | 股票代码、主营业务、产品类型、产品名称、经营范围 |
| `stock_zygc_em` | 东方财富-主营构成 | `symbol`: 如 `"SH688041"` | 股票代码、报告日期、分类类型、主营构成、主营收入（元）、收入比例、主营成本（元）、成本比例、主营利润（元）、利润比例、毛利率 |
| `stock_zh_scale_comparison_em` | 公司规模同行比较 | `symbol`: 如 `"SZ000895"` | 代码、简称、总市值、总市值排名、流通市值、流通市值排名、营业收入、营业收入排名、净利润、净利润排名 |
| `stock_zh_growth_comparison_em` | 成长性同行比较 | `symbol`: 如 `"SZ000895"` | 代码、简称、基本每股收益增长率（3年复合/24A/TTM/25E/26E/27E）、营业收入增长率（3年复合/24A/TTM/25E/26E/27E）、净利润增长率（3年复合/24A/TTM/25E/26E/27E）及对应排名 |
| `stock_zh_valuation_comparison_em` | 估值同行比较 | `symbol`: 如 `"SZ000895"` | 排名、代码、简称、PEG、市盈率（24A/TTM/25E/26E/27E）、市销率（24A/TTM/25E/26E/27E）、市净率（24A/MRQ）、市现率、EV/EBITDA |

#### 1.2.2 港股公司数据（参考）

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_hk_security_profile_em` | 港股证券资料 | 证券代码、证券简称、上市日期、证券类型、发行价、发行量（股）、每手股数、每股面值、交易所、板块、年结日、ISIN、是否沪港通标的 |
| `stock_hk_company_profile_em` | 港股公司资料 | 公司名称、英文名称、注册地、公司成立日期、所属行业、董事长、公司秘书、员工人数、办公地址、公司网址、E-MAIL、年结日、联系电话、核数师、传真、公司介绍 |
| `stock_hk_financial_indicator_em` | 港股财务指标 | 基本每股收益、每股净资产、法定股本、每手股数、每股股息TTM、派息比率、已发行股本、销售净利率、净利润、股东权益回报率、市盈率、市净率、总资产回报率 |

---

### 1.3 财务报表数据

AKShare 提供东方财富和新浪财经两个数据源的财务报表接口。东方财富接口字段更丰富（200~300+ 字段），推荐优先使用。

#### 1.3.1 资产负债表

| 接口名 | 描述 | 输入参数 | 字段数量 |
|--------|------|---------|---------|
| `stock_balance_sheet_by_report_em` | 资产负债表-按报告期（东财） | `symbol`: 如 `"SH600519"` | ~319 列 |
| `stock_balance_sheet_by_yearly_em` | 资产负债表-按年度（东财） | `symbol`: 如 `"SH600519"` | ~319 列 |
| `stock_balance_sheet_by_quarterly_em` | 资产负债表-按单季度（东财） | `symbol`: 如 `"SH600519"` | ~319 列 |
| `stock_zcfz_em` | 资产负债表-旧版（东财） | `date`: 如 `"20241231"` | 约 20 列（汇总） |
| `stock_financial_report_sina` | 新浪财经-三大报表 | `stock`: 如 `"sh600600"`、`symbol`: `"资产负债表"` | ~147 列 |

**`stock_balance_sheet_by_report_em` 核心字段明细**

| 字段名 | 说明 | 字段名 | 说明 |
|--------|------|--------|------|
| SECUCODE | 股票代码(带后缀) | SECURITY_CODE | 股票代码 |
| SECURITY_NAME_ABBR | 股票简称 | REPORT_DATE | 报告日期 |
| REPORT_TYPE | 报告类型 | REPORT_DATE_NAME | 报告日期名称 |
| TOTAL_ASSETS | 资产总计 | TOTAL_LIABILITIES | 负债合计 |
| TOTAL_EQUITY | 股东权益合计 | TOTAL_ASSETS_LIABILITIES | 负债和股东权益总计 |
| MONETARYFUNDS | 货币资金 | NOTES_RECEIVABLE | 应收票据 |
| ACCOUNTS_RECEIVABLE | 应收账款 | PREPAYMENTS | 预付款项 |
| INVENTORIES | 存货 | TOTAL_CURRENT_ASSETS | 流动资产合计 |
| FIXED_ASSETS | 固定资产 | CONSTRUCTION_IN_PROGRESS | 在建工程 |
| INTANGIBLE_ASSETS | 无形资产 | GOODWILL | 商誉 |
| DEFERRED_TAX_ASSETS | 递延所得税资产 | TOTAL_NON_CURRENT_ASSETS | 非流动资产合计 |
| SHORT_TERM_BORROWING | 短期借款 | NOTES_PAYABLE | 应付票据 |
| ACCOUNTS_PAYABLE | 应付账款 | TOTAL_CURRENT_LIABILITIES | 流动负债合计 |
| LONG_TERM_BORROWING | 长期借款 | BONDS_PAYABLE | 应付债券 |
| TOTAL_NON_CURRENT_LIABILITIES | 非流动负债合计 | SHARE_CAPITAL | 实收资本(或股本) |
| CAPITAL_RESERVE | 资本公积 | SURPLUS_RESERVE | 盈余公积 |
| UNDISTRIBUTED_PROFIT | 未分配利润 | MINORITY_INTEREST | 少数股东权益 |

> 完整字段约 319 项，涵盖资产、负债、权益的完整科目及同比环比数据。部分字段为英文缩写，需对照财报科目理解。

#### 1.3.2 利润表

| 接口名 | 描述 | 输入参数 | 字段数量 |
|--------|------|---------|---------|
| `stock_profit_sheet_by_report_em` | 利润表-按报告期（东财） | `symbol`: 如 `"SH600519"` | ~203 列 |
| `stock_profit_sheet_by_yearly_em` | 利润表-按年度（东财） | `symbol`: 如 `"SH600519"` | ~203 列 |
| `stock_profit_sheet_by_quarterly_em` | 利润表-按单季度（东财） | `symbol`: 如 `"SH600519"` | ~203 列 |
| `stock_lrb_em` | 利润表-汇总（东财） | `date`: 如 `"20240331"` | 约 15 列 |
| `stock_financial_report_sina` | 新浪财经-三大报表 | `stock`: 如 `"sh600600"`、`symbol`: `"利润表"` | ~147 列 |

**`stock_profit_sheet_by_report_em` 核心字段明细**

| 字段名 | 说明 | 字段名 | 说明 |
|--------|------|--------|------|
| SECUCODE | 股票代码(带后缀) | SECURITY_CODE | 股票代码 |
| SECURITY_NAME_ABBR | 股票简称 | REPORT_DATE | 报告日期 |
| TOTAL_OPERATE_INCOME | 营业总收入 | OPERATE_INCOME | 营业收入 |
| TOTAL_OPERATE_COST | 营业总成本 | OPERATE_COST | 营业成本 |
| TAXES_AND_SURCHARGES | 税金及附加 | SALES_EXPENSE | 销售费用 |
| MANAGEMENT_EXPENSE | 管理费用 | RESEARCH_EXPENSE | 研发费用 |
| FINANCIAL_EXPENSE | 财务费用 | INTEREST_EXPENSE | 利息费用 |
| INTEREST_INCOME | 利息收入 | ASSETS_IMPAIRMENT_LOSS | 资产减值损失 |
| CREDIT_IMPAIRMENT_LOSS | 信用减值损失 | FAIR_VALUE_CHANGE_INCOME | 公允价值变动收益 |
| INVEST_INCOME | 投资收益 | ASSETS_DISPOSAL_INCOME | 资产处置收益 |
| OTHER_INCOME | 其他收益 | OPERATE_PROFIT | 营业利润 |
| NON_BUSINESS_INCOME | 营业外收入 | NON_BUSINESS_EXPENSE | 营业外支出 |
| TOTAL_PROFIT | 利润总额 | INCOME_TAX | 所得税费用 |
| NETPROFIT | 净利润 | NETPROFIT_PARENT | 归属于母公司股东的净利润 |
| DEDUCT_PARENT_NETPROFIT | 扣除非经常性损益后的净利润 | BASIC_EPS | 基本每股收益 |
| DILUTED_EPS | 稀释每股收益 | OTHER_COMPREHENSIVE_INCOME | 其他综合收益 |
| TOTAL_COMPREHENSIVE_INCOME | 综合收益总额 | TOTAL_COMPREHENSIVE_INCOME_PARENT | 归属于母公司股东的综合收益总额 |

> 完整字段约 203 项，包含各科目同比环比数据。科目命名基本遵循中国会计准则。

#### 1.3.3 现金流量表

| 接口名 | 描述 | 输入参数 | 字段数量 |
|--------|------|---------|---------|
| `stock_cash_flow_sheet_by_report_em` | 现金流量表-按报告期（东财） | `symbol`: 如 `"SH600519"` | ~253 列 |
| `stock_cash_flow_sheet_by_yearly_em` | 现金流量表-按年度（东财） | `symbol`: 如 `"SH600519"` | ~253 列 |
| `stock_cash_flow_sheet_by_quarterly_em` | 现金流量表-按单季度（东财） | `symbol`: 如 `"SH600519"` | ~253 列 |
| `stock_xjll_em` | 现金流量表-汇总（东财） | `date`: 如 `"20241231"` | 约 20 列 |
| `stock_financial_report_sina` | 新浪财经-三大报表 | `stock`: 如 `"sh600600"`、`symbol`: `"现金流量表"` | ~147 列 |

**`stock_cash_flow_sheet_by_report_em` 核心字段明细**

| 字段名 | 说明 | 字段名 | 说明 |
|--------|------|--------|------|
| SECUCODE | 股票代码(带后缀) | SECURITY_CODE | 股票代码 |
| SECURITY_NAME_ABBR | 股票简称 | REPORT_DATE | 报告日期 |
| SALES_SERVICES | 销售商品、提供劳务收到的现金 | TAX_REFUND | 收到的税费返还 |
| OTHER_OPERATE_RECEIVED | 收到其他与经营活动有关的现金 | TOTAL_OPERATE_RECEIVED | 经营活动现金流入小计 |
| GOODS_SERVICES_RECEIVED | 购买商品、接受劳务支付的现金 | EMPLOYEE_PAYMENT | 支付给职工以及为职工支付的现金 |
| TAX_PAYMENT | 支付的各项税费 | OTHER_OPERATE_PAYMENT | 支付其他与经营活动有关的现金 |
| TOTAL_OPERATE_PAYMENT | 经营活动现金流出小计 | NETCASH_OPERATE | 经营活动产生的现金流量净额 |
| WITHDRAW_INVESTMENT | 收回投资收到的现金 | INVEST_INCOME_RECEIVED | 取得投资收益收到的现金 |
| DISPOSAL_LONG_ASSET | 处置固定资产、无形资产和其他长期资产收回的现金净额 | TOTAL_INVEST_RECEIVED | 投资活动现金流入小计 |
| CONSTRUCT_LONG_ASSET | 购建固定资产、无形资产和其他长期资产支付的现金 | INVEST_PAYMENT | 投资支付的现金 |
| TOTAL_INVEST_PAYMENT | 投资活动现金流出小计 | NETCASH_INVEST | 投资活动产生的现金流量净额 |
| RECEIVE_LOAN | 吸收投资收到的现金 | LOAN_RECEIVED | 取得借款收到的现金 |
| BONDS_ISSUED | 发行债券收到的现金 | TOTAL_FINANCE_RECEIVED | 筹资活动现金流入小计 |
| LOAN_REPAYMENT | 偿还债务支付的现金 | DIVIDEND_INTEREST_PAYMENT | 分配股利、利润或偿付利息支付的现金 |
| TOTAL_FINANCE_PAYMENT | 筹资活动现金流出小计 | NETCASH_FINANCE | 筹资活动产生的现金流量净额 |
| EXCHANGE_RATE_EFFECT | 汇率变动对现金及现金等价物的影响 | NET_INCREASE_CASH | 现金及现金等价物净增加额 |
| BEGIN_CASH | 期初现金及现金等价物余额 | END_CASH | 期末现金及现金等价物余额 |
| NETCASH_OPERATE_INDIRECT | 经营活动现金流量净额(间接法) | DEPRECIATION | 固定资产折旧、油气资产折耗、生产性生物资产折旧 |
| AMORTIZATION | 无形资产摊销 | DEFERRED_EXPENSE_AMORT | 长期待摊费用摊销 |
| ASSETS_IMPAIRMENT | 资产减值准备 | FREE_CASHFLOW | 企业自由现金流量 |

> 完整字段约 253 项，包含直接法和间接法数据，以及各科目同比环比。

#### 1.3.4 新浪财经三大报表

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `stock_financial_report_sina` | 新浪财经-三大报表历史数据 | `stock`: 带市场标识如 `"sh600600"`、`symbol`: `"资产负债表"` / `"利润表"` / `"现金流量表"` | 报告日、各报表科目（中文列名）、数据源、是否审计、公告日期、币种、类型（合并期末/合并期初）、更新日期 |

> 特点：返回中文列名的 DataFrame，历史数据时间长（可回溯至 1990 年代），但字段不如东财接口丰富。

---

### 1.4 财务分析指标

| 接口名 | 描述 | 输入参数 | 字段数量 |
|--------|------|---------|---------|
| `stock_financial_analysis_indicator_em` | 财务分析-主要指标（东财） | `symbol`: 如 `"301389.SZ"`、`indicator`: `"按报告期"` / `"按单季度"` | ~140 列 |

**`stock_financial_analysis_indicator_em` 核心字段明细**

| 字段名 | 说明 | 字段名 | 说明 |
|--------|------|--------|------|
| SECUCODE | 股票代码(带后缀) | SECURITY_CODE | 股票代码 |
| SECURITY_NAME_ABBR | 股票简称 | REPORT_DATE | 报告日期 |
| EPSJB | 基本每股收益(元) | EPSKCJB | 扣非每股收益(元) |
| EPSXS | 稀释每股收益(元) | BPS | 每股净资产(元) |
| MGZBGJ | 每股公积金(元) | MGWFPLR | 每股未分配利润(元) |
| MGJYXJJE | 每股经营现金流(元) | TOTALOPERATEREVE | 营业总收入(元) |
| MLR | 毛利润(元) | PARENTNETPROFIT | 归属净利润(元) |
| KCFJCXSYJLR | 扣非净利润(元) | TOTALOPERATEREVETZ | 营业总收入同比增长(%) |
| PARENTNETPROFITTZ | 归属净利润同比增长(%) | KCFJCXSYJLRTZ | 扣非净利润同比增长(%) |
| YYZSRGDHBZC | 营业总收入滚动环比增长(%) | NETPROFITRPHBZC | 归属净利润滚动环比增长(%) |
| KFJLRGDHBZC | 扣非净利润滚动环比增长(%) | ROEJQ | 净资产收益率(加权)(%) |
| ROEKCJQ | 净资产收益率(扣非/加权)(%) | ZZCJLL | 总资产收益率(加权)(%) |
| XSJLL | 净利率(%) | XSMLL | 毛利率(%) |
| YSZKYYSR | 预收账款/营业收入 | XSJXLYYSR | 销售净现金流/营业收入 |
| JYXJLYYSR | 经营净现金流/营业收入 | TAXRATE | 实际税率(%) |
| LD | 流动比率 | SD | 速动比率 |
| XJLLB | 现金流量比率 | ZCFZL | 资产负债率(%) |
| QYCS | 权益系数 | CQBL | 产权比率 |
| ZZCZZTS | 总资产周转天数(天) | CHZZTS | 存货周转天数(天) |
| YSZKZZTS | 应收账款周转天数(天) | TOAZZL | 总资产周转率(次) |
| CHZZL | 存货周转率(次) | YSZKZZL | 应收账款周转率(次) |

> 完整字段约 140 项，覆盖盈利能力、偿债能力、运营能力、成长能力四大维度。每个维度均包含报告期数据和单季度数据。

---

### 1.5 业绩报表与快报

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `stock_yjbb_em` | 业绩报表（东财） | `date`: 如 `"20240331"`（季报：0331/0630/0930/1231） | 序号、股票代码、股票简称、公告日期、基本每股收益(BASIC_EPS)、营业收入(TOTAL_OPERATE_INCOME)、净利润(PARENT_NETPROFIT)、净资产收益率(WEIGHTAVG_ROE)、每股净资产(BPS)、每股现金流(OPERATE_CASH_FLOW_PS)、毛利率(MGJLR)、净利率(JLRL)、营收同比(REVENUE_YOY)、净利润同比(PROFIT_YOY)、扣非净利润(DEDUCT_PARENT_NETPROFIT)、总股本(TOTAL_SHARES)、流通股本(FLOAT_SHARES)、所属板块(BOARD_NAME)、板块代码(BOARD_CODE) |
| `stock_lrb_em` | 利润表-汇总（东财） | `date`: 如 `"20240331"` | 序号、股票代码、股票简称、净利润、净利润同比、营业总收入、营业总收入同比、营业支出、销售费用、管理费用、财务费用、营业总支出、营业利润、利润总额、公告日期 |

> 说明：`stock_yjbb_em` 近期更新后从 35 字段增加到 38 字段，新增 `BOARD_NAME`、`ORI_BOARD_CODE`、`BOARD_CODE` 三个板块相关字段。该接口为**全市场批量**接口，适合一次性获取某一报告期所有上市公司的业绩汇总。

---

## 二、Tushare Pro

Tushare Pro 是 Tushare 的升级版本，采用积分制权限管理，数据质量较高，接口统一规范。

### 2.1 股票基本信息

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `stock_basic` | 股票基础信息 | `ts_code`、`name`、`market`、`list_status`、`exchange`、`is_hs` | ts_code（TS代码）、symbol（股票代码）、name（股票名称）、area（地域）、industry（所属行业）、fullname（股票全称）、enname（英文全称）、cnspell（拼音缩写）、market（市场类型：主板/创业板/科创板/CDR/北交所）、exchange（交易所代码）、curr_type（交易货币）、list_status（上市状态：L上市/D退市/P暂停/G未交易）、list_date（上市日期）、delist_date（退市日期）、is_hs（是否沪深港通标的：N否/H沪股通/S深股通）、act_name（实控人名称）、act_ent_type（实控人企业性质） |
| `namechange` | 股票曾用名 | `ts_code` | ts_code、name（曾用名）、start_date、end_date、ann_date、change_reason（变更原因） |
| `hs_const` | 沪深股通成份股 | `hs_type`: `SH`沪股通/`SZ`深股通 | ts_code、hs_type、in_date、out_date、is_new |
| `stock_company` | 上市公司基本信息 | `ts_code`、`exchange` | ts_code、com_name（公司全称）、com_id（统一社会信用代码）、exchange、chairman（法人代表）、manager（总经理）、secretary（董秘）、reg_capital（注册资本/万元）、setup_date（注册日期）、province（所在省份）、city（所在城市）、introduction（公司介绍）、website（公司主页）、email（电子邮件）、office（办公室）、employees（员工人数）、main_business（主要业务及产品）、business_scope（经营范围） |

> 权限说明：`stock_basic` 2000 积分起，每分钟 50 次。`stock_company` 120 积分起。

---

### 2.2 公司基本信息

#### 2.2.1 `stock_company` 字段明细

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| ts_code | str | 股票代码 | 000001.SZ |
| com_name | str | 公司全称 | 平安银行股份有限公司 |
| com_id | str | 统一社会信用代码 | 91440300192185379H |
| exchange | str | 交易所代码 | SZSE |
| chairman | str | 法人代表 | 谢永林 |
| manager | str | 总经理 | 胡跃飞 |
| secretary | str | 董秘 | 周强 |
| reg_capital | float | 注册资本(万元) | 1717041.0 |
| setup_date | str | 注册日期 | 19871222 |
| province | str | 所在省份 | 广东 |
| city | str | 所在城市 | 深圳 |
| introduction | str | 公司介绍 | 公司是国内首家上市的全国性股份制商业银行... |
| website | str | 公司主页 | www.bank.pingan.com |
| email | str | 电子邮件 | ir@pingan.com.cn |
| office | str | 办公室 | 深圳市福田区益田路5023号平安金融中心B座 |
| employees | int | 员工人数 | 92000 |
| main_business | str | 主要业务及产品 | 吸收公众存款；发放短期、中期和长期贷款；... |
| business_scope | str | 经营范围 | 办理人民币存、贷、结算、汇兑业务；... |

#### 2.2.2 管理层与薪酬

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `stk_managers` | 上市公司管理层 | `ts_code` | ts_code、ann_date、name（姓名）、gender（性别）、lev（岗位类别）、title（岗位）、edu（学历）、nationality（国籍）、birthday（出生年月）、begin_date（上任日期）、end_date（离任日期）、resume（简历） |
| `stk_rewards` | 管理层薪酬和持股 | `ts_code` | ts_code、ann_date、end_date、name（姓名）、title（职务）、reward（报酬/万元）、hold_vol（持股数） |

---

### 2.3 财务报表数据

Tushare Pro 的财务报表接口采用标准会计准则字段命名，数据规范，适合批量采集和结构化存储。

#### 2.3.1 利润表

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `income` | 上市公司利润表 | `ts_code`（必选）、`ann_date`、`start_date`、`end_date`、`period`、`report_type`、`comp_type` | ts_code、ann_date（公告日期）、f_ann_date（实际公告日期）、end_date（报告期）、report_type（报告类型：1合并报表/2单季合并/3调整单季合并/4调整合并/5调整前合并/6母公司报表/7母公司单季表/8母公司调整单季表/9母公司调整表/10母公司调整前报表）、comp_type（公司类型：1一般工商业/2银行/3保险/4证券）、basic_eps（基本每股收益）、diluted_eps（稀释每股收益）、total_revenue（营业总收入）、revenue（营业收入）、int_income（利息收入）、prem_earned（已赚保费）、comm_income（手续费及佣金收入）、n_commis_income（手续费及佣金净收入）、n_oth_income（其他经营净收益）、fv_value_chg_gain（公允价值变动净收益）、invest_income（投资净收益）、ass_invest_income（对联营企业和合营企业的投资收益）、forex_gain（汇兑净收益）、total_cogs（营业总成本）、oper_cost（营业成本）、int_exp（利息支出）、comm_exp（手续费及佣金支出）、biz_tax_surchg（营业税金及附加）、sell_exp（销售费用）、admin_exp（管理费用）、fin_exp（财务费用）、assets_impair_loss（资产减值损失）、operate_profit（营业利润）、non_oper_income（营业外收入）、non_oper_exp（营业外支出）、total_profit（利润总额）、income_tax（所得税费用）、n_income（净利润/含少数股东损益）、n_income_attr_p（净利润/不含少数股东损益，即归母净利润）、minority_gain（少数股东损益）、oth_compr_income（其他综合收益）、t_compr_income（综合收益总额）、compr_inc_attr_p（归属于母公司的综合收益总额）、ebit（息税前利润）、ebitda（息税折旧摊销前利润）、undist_profit（年初未分配利润）、distable_profit（可分配利润）、rd_exp（研发费用）、credit_impa_loss（信用减值损失）、oth_income（其他收益）、asset_disp_income（资产处置收益）、continued_net_profit（持续经营净利润）、end_net_profit（终止经营净利润）、update_flag（更新标识） |

#### 2.3.2 资产负债表

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `balancesheet` | 上市公司资产负债表 | `ts_code`（必选）、`ann_date`、`start_date`、`end_date`、`period`、`report_type`、`comp_type` | ts_code、ann_date、f_ann_date、end_date、report_type、comp_type、total_share（期末总股本）、cap_rese（资本公积金）、undistr_porfit（未分配利润）、surplus_rese（盈余公积金）、special_rese（专项储备）、money_cap（货币资金）、trad_asset（交易性金融资产）、notes_receiv（应收票据）、accounts_receiv（应收账款）、oth_receiv（其他应收款）、prepayment（预付款项）、div_receiv（应收股利）、int_receiv（应收利息）、inventories（存货）、total_cur_assets（流动资产合计）、fa_avail_for_sale（可供出售金融资产）、htm_invest（持有至到期投资）、lt_eqt_invest（长期股权投资）、invest_real_estate（投资性房地产）、lt_rec（长期应收款）、fix_assets（固定资产）、cip（在建工程）、produc_bio_assets（生产性生物资产）、intan_assets（无形资产）、r_and_d（研发支出）、goodwill（商誉）、lt_amor_exp（长期待摊费用）、defer_tax_assets（递延所得税资产）、total_nca（非流动资产合计）、total_assets（资产总计）、lt_borr（长期借款）、st_borr（短期借款）、notes_payable（应付票据）、acct_payable（应付账款）、adv_receipts（预收款项）、payroll_payable（应付职工薪酬）、taxes_payable（应交税费）、oth_payable（其他应付款）、total_cur_liab（流动负债合计）、bond_payable（应付债券）、lt_payable（长期应付款）、estimated_liab（预计负债）、defer_tax_liab（递延所得税负债）、total_ncl（非流动负债合计）、total_liab（负债合计）、treasury_share（库存股）、minority_int（少数股东权益）、total_hldr_eqy_exc_min_int（股东权益合计/不含少数股东权益）、total_hldr_eqy_inc_min_int（股东权益合计/含少数股东权益）、total_liab_hldr_eqy（负债及股东权益总计）、oth_comp_income（其他综合收益）、contract_assets（合同资产）、contract_liab（合同负债）、use_right_assets（使用权资产）、lease_liab（租赁负债）、accounts_receiv_bill（应收票据及应收账款）、accounts_pay（应付票据及应付账款）、oth_rcv_total（其他应收款合计）、fix_assets_total（固定资产合计）、update_flag |

#### 2.3.3 现金流量表

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `cashflow` | 上市公司现金流量表 | `ts_code`（必选）、`ann_date`、`start_date`、`end_date`、`period`、`report_type`、`comp_type`、`is_calc` | ts_code、ann_date、f_ann_date、end_date、comp_type、report_type、net_profit（净利润）、finan_exp（财务费用）、c_fr_sale_sg（销售商品、提供劳务收到的现金）、recp_tax_rends（收到的税费返还）、c_inf_fr_operate_a（经营活动现金流入小计）、c_paid_goods_s（购买商品、接受劳务支付的现金）、c_paid_to_for_empl（支付给职工以及为职工支付的现金）、c_paid_for_taxes（支付的各项税费）、st_cash_out_act（经营活动现金流出小计）、n_cashflow_act（经营活动产生的现金流量净额）、c_disp_withdrwl_invest（收回投资收到的现金）、c_recp_return_invest（取得投资收益收到的现金）、stot_inflows_inv_act（投资活动现金流入小计）、c_pay_acq_const_fiolta（购建固定资产、无形资产和其他长期资产支付的现金）、c_paid_invest（投资支付的现金）、stot_out_inv_act（投资活动现金流出小计）、n_cashflow_inv_act（投资活动产生的现金流量净额）、c_recp_borrow（取得借款收到的现金）、proc_issue_bonds（发行债券收到的现金）、stot_cash_in_fnc_act（筹资活动现金流入小计）、c_prepay_amt_borr（偿还债务支付的现金）、c_pay_dist_dpcp_int_exp（分配股利、利润或偿付利息支付的现金）、stot_cashout_fnc_act（筹资活动现金流出小计）、n_cash_flows_fnc_act（筹资活动产生的现金流量净额）、eff_fx_flu_cash（汇率变动对现金的影响）、n_incr_cash_cash_equ（现金及现金等价物净增加额）、c_cash_equ_beg_period（期初现金及现金等价物余额）、c_cash_equ_end_period（期末现金及现金等价物余额）、free_cashflow（企业自由现金流量）、prov_depr_assets（资产减值准备）、depr_fa_coga_dpba（固定资产折旧）、amort_intang_assets（无形资产摊销）、im_net_cashflow_oper_act（经营活动产生的现金流量净额/间接法）、update_flag |

> 权限说明：`income`、`balancesheet`、`cashflow` 均需 2000 积分起。单只股票获取历史数据，如需获取某一季度全部上市公司数据，需使用 `*_vip` 接口（5000 积分）。

---

### 2.4 财务指标数据

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `fina_indicator` | 上市公司财务指标数据 | `ts_code`（必选）、`ann_date`、`start_date`、`end_date`、`period` | ts_code、ann_date、end_date（报告期）、eps（基本每股收益）、dt_eps（稀释每股收益）、total_revenue_ps（每股营业总收入）、revenue_ps（每股营业收入）、bps（每股净资产）、ocfps（每股经营活动产生的现金流量净额）、cfps（每股现金流量净额）、ebit_ps（每股息税前利润）、fcff_ps（每股企业自由现金流量）、fcfe_ps（每股股东自由现金流量）、current_ratio（流动比率）、quick_ratio（速动比率）、cash_ratio（保守速动比率）、inv_turn（存货周转率）、ar_turn（应收账款周转率）、ca_turn（流动资产周转率）、fa_turn（固定资产周转率）、assets_turn（总资产周转率）、ebit（息税前利润）、ebitda（息税折旧摊销前利润）、fcff（企业自由现金流量）、fcfe（股权自由现金流量）、netprofit_margin（销售净利率）、grossprofit_margin（销售毛利率）、cogs_of_sales（销售成本率）、expense_of_sales（销售期间费用率）、profit_to_gr（净利润/营业总收入）、saleexp_to_gr（销售费用/营业总收入）、adminexp_of_gr（管理费用/营业总收入）、finaexp_of_gr（财务费用/营业总收入）、gc_of_gr（营业总成本/营业总收入）、op_of_gr（营业利润/营业总收入）、ebit_of_gr（息税前利润/营业总收入）、roe（净资产收益率）、roe_waa（加权平均净资产收益率）、roe_dt（净资产收益率/扣除非经常损益）、roa（总资产报酬率）、npta（总资产净利润）、roic（投入资本回报率）、roe_yearly（年化净资产收益率）、roa2_yearly（年化总资产报酬率）、debt_to_assets（资产负债率）、assets_to_eqt（权益乘数）、dp_assets_to_eqt（权益乘数/杜邦分析）、ca_to_assets（流动资产/总资产）、nca_to_assets（非流动资产/总资产）、tbassets_to_totalassets（有形资产/总资产）、int_to_talcap（带息债务/全部投入资本）、eqt_to_talcapital（归属于母公司的股东权益/全部投入资本）、currentdebt_to_debt（流动负债/负债合计）、longdeb_to_debt（非流动负债/负债合计）、ocf_to_shortdebt（经营活动产生的现金流量净额/流动负债）、debt_to_eqt（产权比率）、eqt_to_debt（归属于母公司的股东权益/负债合计）、eqt_to_interestdebt（归属于母公司的股东权益/带息债务）、tangibleasset_to_debt（有形资产/负债合计）、tangasset_to_intdebt（有形资产/带息债务）、tangibleasset_to_netdebt（有形资产/净债务）、ocf_to_debt（经营活动产生的现金流量净额/负债合计）、ocf_to_interestdebt（经营活动产生的现金流量净额/带息债务）、ocf_to_netdebt（经营活动产生的现金流量净额/净债务）、ebit_to_interest（已获利息倍数）、ebitda_to_debt（息税折旧摊销前利润/负债合计）、turn_days（营业周期）、roa_yearly（年化总资产净利率）、roa_dp（总资产净利率/杜邦分析）、fixed_assets（固定资产合计）、profit_prefin_exp（扣除财务费用前营业利润）、op_to_ebt（营业利润/利润总额）、nop_to_ebt（非营业利润/利润总额）、ocf_to_profit（经营活动产生的现金流量净额/营业利润）、cash_to_liqdebt（货币资金/流动负债）、op_to_liqdebt（营业利润/流动负债）、op_to_debt（营业利润/负债合计）、roic_yearly（年化投入资本回报率）、total_fa_trun（固定资产合计周转率）、profit_to_op（利润总额/营业收入）、q_opincome（经营活动单季度净收益）、q_investincome（价值变动单季度净收益）、q_dtprofit（扣除非经常损益后的单季度净利润）、q_eps（每股收益/单季度）、q_netprofit_margin（销售净利率/单季度）、q_gsprofit_margin（销售毛利率/单季度）、q_exp_to_sales（销售期间费用率/单季度）、q_profit_to_gr（净利润/营业总收入/单季度）、q_saleexp_to_gr（销售费用/营业总收入/单季度）、q_adminexp_to_gr（管理费用/营业总收入/单季度）、q_finaexp_to_gr（财务费用/营业总收入/单季度）、q_gc_to_gr（营业总成本/营业总收入/单季度）、q_op_to_gr（营业利润/营业总收入/单季度）、q_roe（净资产收益率/单季度）、q_dt_roe（净资产单季度收益率/扣非）、q_npta（总资产净利润/单季度）、q_opincome_to_ebt（经营活动净收益/利润总额/单季度）、q_investincome_to_ebt（价值变动净收益/利润总额/单季度）、q_dtprofit_to_profit（扣非净利润/净利润/单季度）、q_salescash_to_or（销售商品提供劳务收到的现金/营业收入/单季度）、q_ocf_to_sales（经营活动产生的现金流量净额/营业收入/单季度）、basic_eps_yoy（基本每股收益同比增长率%）、dt_eps_yoy（稀释每股收益同比增长率%）、cfps_yoy（每股经营活动产生的现金流量净额同比增长率%）、op_yoy（营业利润同比增长率%）、ebt_yoy（利润总额同比增长率%）、netprofit_yoy（归属母公司股东的净利润同比增长率%）、dt_netprofit_yoy（扣非净利润同比增长率%）、ocf_yoy（经营活动产生的现金流量净额同比增长率%）、roe_yoy（净资产收益率同比增长率%）、bps_yoy（每股净资产相对年初增长率%）、assets_yoy（资产总计相对年初增长率%）、eqt_yoy（归属母公司的股东权益相对年初增长率%）、tr_yoy（营业总收入同比增长率%）、or_yoy（营业收入同比增长率%）、q_gr_yoy（营业总收入同比增长率%/单季度）、q_gr_qoq（营业总收入环比增长率%/单季度）、q_sales_yoy（营业收入同比增长率%/单季度）、q_sales_qoq（营业收入环比增长率%/单季度）、q_op_yoy（营业利润同比增长率%/单季度）、q_op_qoq（营业利润环比增长率%/单季度）、q_profit_yoy（净利润同比增长率%/单季度）、q_profit_qoq（净利润环比增长率%/单季度）、q_netprofit_yoy（归属母公司股东的净利润同比增长率%/单季度）、q_netprofit_qoq（归属母公司股东的净利润环比增长率%/单季度）、equity_yoy（净资产同比增长率%）、rd_exp（研发费用）、update_flag |

> 提示：`fina_indicator` 现阶段每次请求最多返回 100 条记录，可通过设置日期多次请求获取更多数据。如需获取某一季度全部上市公司数据，需使用 `fina_indicator_vip` 接口（5000 积分）。该接口包含约 150+ 个指标字段，覆盖盈利能力、偿债能力、运营能力、成长能力、现金流能力等维度，且支持单季度数据和同比环比计算。

---

### 2.5 业绩预告

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `forecast` | 业绩预告 | `ts_code`、`ann_date`、`start_date`、`end_date`、`period` | ts_code、ann_date（公告日期）、end_date（报告期）、type（业绩预告类型：预增/预减/扭亏/首亏/续亏/续盈/略增/略减）、p_change_min（预告净利润变动幅度下限%）、p_change_max（预告净利润变动幅度上限%）、net_profit_min（预告净利润下限/万元）、net_profit_max（预告净利润上限/万元）、last_parent_net（上年同期归属母公司净利润）、first_ann_date（首次公告日）、summary（业绩预告摘要）、change_reason（业绩变动原因） |

> 权限：2000 积分起。如需获取某一季度全部上市公司数据，需使用 `forecast_vip` 接口（5000 积分）。

---

## 三、接口对比与采集建议

### 3.1 股票基本信息对比

| 能力 | AKShare | Tushare Pro |
|------|---------|-------------|
| 全市场股票列表 | `stock_info_a_code_name`（无限制） | `stock_basic`（2000积分起） |
| 个股详细信息 | `stock_individual_info_em`（东财） | 无单独接口 |
| 公司全称/英文名称 | `stock_profile_cninfo`（巨潮） | `stock_basic` |
| 法人代表/注册资本 | `stock_profile_cninfo` | `stock_company` |
| 实控人/企业性质 | `stock_individual_basic_info_xq` | `stock_basic` |
| 主营业务/经营范围 | `stock_profile_cninfo`、`stock_zyjs_ths` | `stock_company` |
| 联系方式/地址 | `stock_profile_cninfo` | `stock_company` |
| 员工人数 | `stock_individual_basic_info_xq` | `stock_company` |
| 上市状态/退市日期 | — | `stock_basic` |
| 沪深港通标的 | — | `stock_basic`、`hs_const` |
| 曾用名 | — | `namechange` |
| 管理层信息 | — | `stk_managers` |
| 管理层薪酬 | — | `stk_rewards` |

### 3.2 财务报表对比

| 能力 | AKShare | Tushare Pro |
|------|---------|-------------|
| 资产负债表 | `stock_balance_sheet_by_report_em`（~319列，东财） | `balancesheet`（2000积分起） |
| 利润表 | `stock_profit_sheet_by_report_em`（~203列，东财） | `income`（2000积分起） |
| 现金流量表 | `stock_cash_flow_sheet_by_report_em`（~253列，东财） | `cashflow`（2000积分起） |
| 三大报表（中文列名） | `stock_financial_report_sina` | 无 |
| 业绩报表（全市场批量） | `stock_yjbb_em`（无限制） | 无对应接口 |
| 财务分析指标 | `stock_financial_analysis_indicator_em`（~140列） | `fina_indicator`（~150+列，2000积分起） |
| 业绩预告 | — | `forecast`（2000积分起） |
| 单季度数据 | 支持（东财接口） | 支持（`report_type=2`） |
| 同比环比 | 支持（东财接口） | 支持（`fina_indicator`） |
| 报告类型选择 | 支持（按报告期/年度/单季度） | 支持（1-12种报告类型） |
| 公司类型区分 | 不支持 | 支持（一般工商业/银行/保险/证券） |

### 3.3 采集建议

| 目标数据 | 推荐接口 | 说明 |
|---------|---------|------|
| **股票主表（代码、名称、行业）** | AKShare `stock_info_a_code_name` + Tushare `stock_basic` | AKShare 无限制，Tushare 数据规范，可互补校验 |
| **公司静态信息（法人、地址、主营业务）** | AKShare `stock_profile_cninfo` | 信息最全面，含沿革、联系方式 |
| **公司管理层信息** | Tushare `stk_managers` + `stk_rewards` | 包含简历、薪酬、持股 |
| **注册资本/员工人数/实控人** | Tushare `stock_company` + `stock_basic` | 数据结构化程度高 |
| **实时估值（总市值、PE、PB）** | AKShare `stock_zh_a_spot_em` | 无限制，实时更新 |
| **每日估值指标批量** | Tushare `daily_basic` | 2000积分起，适合批量更新 |
| **三大报表完整数据** | AKShare `stock_*_sheet_by_report_em`（东财） | 字段丰富（200~300+），免费无限制 |
| **三大报表标准科目** | Tushare `income`/`balancesheet`/`cashflow` | 字段命名规范，适合结构化存储 |
| **财务指标分析** | Tushare `fina_indicator` | 150+ 指标，含单季度和同比环比 |
| **业绩报表批量获取** | AKShare `stock_yjbb_em` | 全市场批量，适合定期更新 |
| **业绩预告** | Tushare `forecast` | 需 2000 积分 |
| **主营业务构成** | AKShare `stock_zygc_em` | 按产品/行业分类的收入构成 |

### 3.4 积分与权限参考（Tushare）

| 接口 | 最低积分 | 频次限制 | 备注 |
|------|---------|---------|------|
| `stock_basic` | 2000 | 50次/分钟 | 建议本地持久化 |
| `stock_company` | 120 | — | 基本信息 |
| `income` | 2000 | — | 利润表 |
| `balancesheet` | 2000 | — | 资产负债表 |
| `cashflow` | 2000 | — | 现金流量表 |
| `fina_indicator` | 2000 | — | 每次最多100条 |
| `forecast` | 2000 | — | 业绩预告 |
| `*_vip` | 5000 | — | 全市场批量 |

---

> 附录：
> - AKShare 官方文档：https://akshare.akfamily.xyz/
> - Tushare Pro 官方文档：https://www.tushare.pro/document/2
