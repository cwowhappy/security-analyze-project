# 数据源接口手册：AKShare & Tushare

> 本文档整理 AKShare 与 Tushare 在股票行情数据与公司基本面数据方面的主要获取接口及返回字段，供证券分析系统数据采集模块（collector）参考选型。
> 整理日期：2026-05-10

---

## 目录

- [一、AKShare](#一akshare)
  - [1.1 股票行情数据](#11-股票行情数据)
  - [1.2 公司数据](#12-公司数据)
- [二、Tushare Pro](#二tushare-pro)
  - [2.1 股票行情数据](#21-股票行情数据)
  - [2.2 公司数据](#22-公司数据)
- [三、对比速查](#三对比速查)

---

## 一、AKShare

AKShare 是基于 Python 的开源金融数据接口库，主要数据源为东方财富、同花顺、新浪财经、腾讯证券等公开数据。

### 1.1 股票行情数据

#### 1.1.1 实时行情数据

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_zh_a_spot_em` | 沪深京 A 股实时行情（东方财富） | 序号、代码、名称、最新价、涨跌幅、涨跌额、成交量（手）、成交额（元）、振幅、最高、最低、今开、昨收、量比、换手率、市盈率-动态、市净率、总市值、流通市值、涨速、5分钟涨跌、60日涨跌幅、年初至今涨跌幅 |
| `stock_sh_a_spot_em` | 沪 A 股实时行情 | 同上 |
| `stock_sz_a_spot_em` | 深 A 股实时行情 | 同上 |
| `stock_bj_a_spot_em` | 京 A 股（北交所）实时行情 | 同上 |
| `stock_cy_a_spot_em` | 创业板实时行情 | 同上 |
| `stock_kc_a_spot_em` | 科创板实时行情 | 同上 |
| `stock_new_a_spot_em` | 新股实时行情 | 同上 + 上市时间 |
| `stock_zh_a_spot` | 沪深京 A 股实时行情（新浪） | 代码、名称、最新价、涨跌额、涨跌幅、买入、卖出、昨收、今开、最高、最低、成交量（股）、成交额（元）、时间戳 |
| `stock_individual_spot_xq` | 个股实时行情（雪球） | 代码、52周最高、流通股、跌停、最高、流通值、最小交易单位、涨跌、每股收益、昨收、成交量、周转率、52周最低、名称、交易所、市盈率(动)、基金份额/总股本、净资产中的商誉、均价、涨幅、振幅、现价、今年以来涨幅、发行日期、最低、资产净值/总市值、股息(TTM)、股息率(TTM)、货币、每股净资产、市盈率(静)、成交额、市净率、涨停、市盈率(TTM)、时间、今开 |

#### 1.1.2 历史行情数据

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_zh_a_hist` | 沪深京 A 股日频率历史数据（东财，推荐） | 日期、股票代码、开盘、收盘、最高、最低、成交量（手）、成交额（元）、振幅、涨跌幅、涨跌额、换手率 |
| `stock_zh_a_daily` | 沪深京 A 股历史数据（新浪） | date、open、high、low、close、volume（股）、amount（元）、outstanding_share（流动股本）、turnover |
| `stock_zh_a_hist_tx` | 日频历史数据（腾讯证券） | date、open、close、high、low、amount（手） |

> 复权支持：`stock_zh_a_hist` 支持 `adjust="qfq"`（前复权）、`adjust="hfq"`（后复权）。量化研究通常采用后复权数据。

#### 1.1.3 分时数据

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_zh_a_minute` | 分钟级分时数据（新浪，1/5/15/30/60分钟） | day、open、high、low、close、volume、amount |
| `stock_zh_a_hist_min_em` | 分钟级分时数据（东财，1/5/15/30/60分钟） | 时间、开盘、收盘、最高、最低、成交量（手）、成交额、均价（1分钟）/ 涨跌幅、涨跌额、振幅、换手率（其他周期） |
| `stock_intraday_em` | 日内分时成交明细（东财） | 时间、成交价、手数、买卖盘性质（买盘/卖盘/中性盘） |
| `stock_intraday_sina` | 日内分时成交明细（新浪） | 类似字段 |

#### 1.1.4 市场总貌

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_sse_summary` | 上海证券交易所总貌 | 项目、股票、科创板、主板（流通股本、总市值、平均市盈率、上市公司数、上市股票数、流通市值、报告时间、总股本） |
| `stock_szse_summary` | 深圳证券交易所-证券类别统计 | 证券类别、数量（只）、成交金额（元）、总市值、流通市值 |
| `stock_szse_area_summary` | 深交所-地区交易排序 | 序号、地区、总交易额、占市场（%）、股票交易额、基金交易额、债券交易额、优先股交易额、期权交易额 |
| `stock_szse_sector_summary` | 深交所-股票行业成交 | 项目名称、项目名称-英文、交易天数、成交金额、成交金额占比、成交股数、成交股数占比、成交笔数、成交笔数占比 |
| `stock_sse_deal_daily` | 上交所-每日成交概况 | 单日情况、股票、主板A、主板B、科创板、股票回购（挂牌数、市价总值、流通市值、成交金额、成交量、平均市盈率、换手率、流通换手率） |

#### 1.1.5 行情报价

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_bid_ask_em` | 五档行情报价（东财） | sell_5 ~ sell_1、sell_5_vol ~ sell_1_vol、buy_1 ~ buy_5、buy_1_vol ~ buy_5_vol、最新、均价、涨幅、涨跌、总手、金额、换手、量比、最高、最低、今开、昨收、涨停、跌停、外盘、内盘 |

---

### 1.2 公司数据

#### 1.2.1 个股基本信息

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_individual_info_em` | 个股信息（东财） | item/value 形式：最新价、股票代码、股票简称、总股本、流通股、总市值、流通市值、行业、上市时间 |
| `stock_individual_basic_info_xq` | 公司概况（雪球） | item/value 形式：org_id、org_name_cn（公司全称）、org_short_name_cn（简称）、org_name_en、main_operation_business（主营业务）、operating_scope（经营范围）、legal_representative（法定代表人）、general_manager（总经理）、secretary（董秘）、established_date（成立日期）、reg_asset（注册资本）、staff_num（员工人数）、telephone、email、org_website、reg_address_cn（注册地址）、office_address_cn（办公地址）、actual_controller（实际控制人）、classi_name（企业性质）、chairman（董事长）、issue_price（发行价）、actual_issue_vol（实际发行量）、pe_after_issuing（发行市盈率）、affiliate_industry（所属行业） |

#### 1.2.2 主营与行业数据

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_zyjs_ths` | 主营介绍（同花顺） | 股票代码、主营业务、产品类型、产品名称、经营范围 |
| `stock_zygc_em` | 主营构成（东财） | 股票代码、报告日期、分类类型、主营构成、主营收入（元）、收入比例、主营成本（元）、成本比例、主营利润（元）、利润比例、毛利率 |
| `stock_zh_scale_comparison_em` | 公司规模同行比较（东财） | 代码、简称、总市值、总市值排名、流通市值、流通市值排名、营业收入、营业收入排名、净利润、净利润排名 |
| `stock_zh_growth_comparison_em` | 成长性同行比较（东财） | 代码、简称、基本每股收益增长率（3年复合/24A/TTM/25E/26E/27E）、营业收入增长率（3年复合/24A/TTM/25E/26E/27E）、净利润增长率（3年复合/24A/TTM/25E/26E/27E）及对应排名 |

#### 1.2.3 财务报表数据

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_yjbb_em` | 业绩报表（东财） | 约 38 个字段，包括：股票代码、股票简称、公告日期、基本每股收益(BASIC_EPS)、营业收入(TOTAL_OPERATE_INCOME)、净利润(PARENT_NETPROFIT)、净资产收益率(WEIGHTAVG_ROE)、每股净资产(BPS)、每股现金流(OPERATE_CASH_FLOW_PS)、毛利率(MGJLR)、净利率(JLRL)、营收同比(REVENUE_YOY)、净利润同比(PROFIT_YOY)、扣非净利润(DEDUCT_PARENT_NETPROFIT)、总股本(TOTAL_SHARES)、流通股本(FLOAT_SHARES)、所属板块(BOARD_NAME)、板块代码(BOARD_CODE) 等 |

> 注：`stock_yjbb_em` 近期更新后从 35 字段增加到 38 字段，新增 `BOARD_NAME`、`ORI_BOARD_CODE`、`BOARD_CODE` 三个板块相关字段。

#### 1.2.4 港股公司数据

| 接口名 | 描述 | 主要返回字段 |
|--------|------|-------------|
| `stock_hk_security_profile_em` | 港股证券资料 | 证券代码、证券简称、上市日期、证券类型、发行价、发行量（股）、每手股数、每股面值、交易所、板块、年结日、ISIN、是否沪港通标的 |
| `stock_hk_company_profile_em` | 港股公司资料 | 公司名称、英文名称、注册地、公司成立日期、所属行业、董事长、公司秘书、员工人数、办公地址、公司网址、E-MAIL、年结日、联系电话、核数师、传真、公司介绍 |
| `stock_hk_financial_indicator_em` | 港股财务指标 | 基本每股收益、每股净资产、法定股本、每手股数、每股股息TTM、派息比率、已发行股本、销售净利率、净利润、净利润滚动环比增长、股东权益回报率、市盈率、市净率、总资产回报率等 |
| `stock_hk_dividend_payout_em` | 港股分红派息 | 最新公告日期、财政年度、分红方案、分配类型、除净日、截至过户日、发放日 |

---

## 二、Tushare Pro

Tushare Pro 是 Tushare 的升级版本，采用积分制权限管理，数据质量较高，接口统一规范。

### 2.1 股票行情数据

#### 2.1.1 股票基础信息

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `stock_basic` | 股票基础信息 | ts_code、name、market、list_status、exchange、is_hs | ts_code（TS代码）、symbol（股票代码）、name（股票名称）、area（地域）、industry（所属行业）、fullname（股票全称）、enname（英文全称）、cnspell（拼音缩写）、market（市场类型：主板/创业板/科创板/CDR）、exchange（交易所代码）、curr_type（交易货币）、list_status（上市状态：L上市/D退市/P暂停/G未交易）、list_date（上市日期）、delist_date（退市日期）、is_hs（是否沪深港通标的）、act_name（实控人名称）、act_ent_type（实控人企业性质） |

#### 2.1.2 日线/周线/月线行情

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `daily` | 日线行情（未复权） | ts_code、trade_date、start_date、end_date | ts_code、trade_date、open、high、low、close、pre_close（昨收价/除权价）、change（涨跌额）、pct_chg（涨跌幅%）、vol（成交量/手）、amount（成交额/千元） |
| `weekly` | 周线行情 | 同上 | 同上 |
| `monthly` | 月线行情 | 同上 | 同上 |
| `pro_bar` | 通用行情接口（整合股票/指数/基金/期货/期权，支持复权） | ts_code、start_date、end_date、freq、adj、ma | 根据复权类型返回 open/high/low/close/vol/amount，支持前复权(qfq)/后复权(hfq)，可叠加均线 |

> 说明：`daily` 接口每日 15~16 点之间入库，停牌期间不提供数据。基础积分每分钟可调取 500 次，每次 6000 条。

#### 2.1.3 每日基本面指标

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `daily_basic` | 每日重要基本面指标 | ts_code、trade_date、start_date、end_date | ts_code、trade_date、close（当日收盘价）、turnover_rate（换手率%）、turnover_rate_f（自由流通股换手率）、volume_ratio（量比）、pe（市盈率）、pe_ttm（市盈率TTM）、pb（市净率）、ps（市销率）、ps_ttm（市销率TTM）、dv_ratio（股息率%）、dv_ttm（股息率TTM）、total_share（总股本/万股）、float_share（流通股本/万股）、free_share（自由流通股本/万）、total_mv（总市值/万元）、circ_mv（流通市值/万元） |

#### 2.1.4 资金流向

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `moneyflow` | 个股资金流向（大单小单分析） | ts_code、trade_date、start_date、end_date | ts_code、trade_date、buy_sm_vol（小单买入量/手）、buy_sm_amount（小单买入金额/万元）、sell_sm_vol、sell_sm_amount、buy_md_vol（中单买入量）、buy_md_amount、sell_md_vol、sell_md_amount、buy_lg_vol（大单买入量）、buy_lg_amount、sell_lg_vol、sell_lg_amount、buy_elg_vol（特大单买入量）、buy_elg_amount、sell_elg_vol、sell_elg_amount、net_mf_vol（净流入量/手）、net_mf_amount（净流入额/万元） |

> 统计规则：小单 < 5万、中单 5~20万、大单 20~100万、特大单 >= 100万。基于主动买卖单统计。数据始于 2010 年。

---

### 2.2 公司数据

#### 2.2.1 利润表

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `income` | 上市公司利润表 | ts_code、ann_date、start_date、end_date、period、report_type、comp_type | ts_code、ann_date（公告日期）、f_ann_date（实际公告日期）、end_date（报告期）、report_type（报告类型：1合并报表/2单季合并/4调整合并等）、comp_type（公司类型：1一般工商业/2银行/3保险/4证券）、basic_eps（基本每股收益）、diluted_eps（稀释每股收益）、total_revenue（营业总收入）、revenue（营业收入）、int_income（利息收入）、prem_earned（已赚保费）、comm_income（手续费及佣金收入）、n_commis_income（手续费及佣金净收入）、n_oth_income（其他经营净收益）、fv_value_chg_gain（公允价值变动净收益）、invest_income（投资净收益）、ass_invest_income（对联营企业和合营企业的投资收益）、forex_gain（汇兑净收益）、total_cogs（营业总成本）、oper_cost（营业成本）、int_exp（利息支出）、comm_exp（手续费及佣金支出）、biz_tax_surchg（营业税金及附加）、sell_exp（销售费用）、admin_exp（管理费用）、fin_exp（财务费用）、assets_impair_loss（资产减值损失）、operate_profit（营业利润）、non_oper_income（营业外收入）、non_oper_exp（营业外支出）、total_profit（利润总额）、income_tax（所得税费用）、n_income（净利润/含少数股东损益）、n_income_attr_p（净利润/不含少数股东损益，即归母净利润）、minority_gain（少数股东损益）、oth_compr_income（其他综合收益）、t_compr_income（综合收益总额）、compr_inc_attr_p（归属于母公司的综合收益总额）、ebit（息税前利润）、ebitda（息税折旧摊销前利润）、undist_profit（年初未分配利润）、distable_profit（可分配利润）、rd_exp（研发费用）、credit_impa_loss（信用减值损失）、oth_income（其他收益）、asset_disp_income（资产处置收益）、continued_net_profit（持续经营净利润）、end_net_profit（终止经营净利润）、update_flag（更新标识） |

#### 2.2.2 资产负债表

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `balancesheet` | 上市公司资产负债表 | ts_code、ann_date、start_date、end_date、period、report_type、comp_type | ts_code、ann_date、f_ann_date、end_date、report_type、comp_type、total_share（期末总股本）、cap_rese（资本公积金）、undistr_porfit（未分配利润）、surplus_rese（盈余公积金）、special_rese（专项储备）、money_cap（货币资金）、trad_asset（交易性金融资产）、notes_receiv（应收票据）、accounts_receiv（应收账款）、oth_receiv（其他应收款）、prepayment（预付款项）、div_receiv（应收股利）、int_receiv（应收利息）、inventories（存货）、total_cur_assets（流动资产合计）、fa_avail_for_sale（可供出售金融资产）、htm_invest（持有至到期投资）、lt_eqt_invest（长期股权投资）、invest_real_estate（投资性房地产）、lt_rec（长期应收款）、fix_assets（固定资产）、cip（在建工程）、produc_bio_assets（生产性生物资产）、intan_assets（无形资产）、r_and_d（研发支出）、goodwill（商誉）、lt_amor_exp（长期待摊费用）、defer_tax_assets（递延所得税资产）、total_nca（非流动资产合计）、total_assets（资产总计）、lt_borr（长期借款）、st_borr（短期借款）、notes_payable（应付票据）、acct_payable（应付账款）、adv_receipts（预收款项）、payroll_payable（应付职工薪酬）、taxes_payable（应交税费）、oth_payable（其他应付款）、total_cur_liab（流动负债合计）、bond_payable（应付债券）、lt_payable（长期应付款）、estimated_liab（预计负债）、defer_tax_liab（递延所得税负债）、total_ncl（非流动负债合计）、total_liab（负债合计）、treasury_share（库存股）、minority_int（少数股东权益）、total_hldr_eqy_exc_min_int（股东权益合计/不含少数股东权益）、total_hldr_eqy_inc_min_int（股东权益合计/含少数股东权益）、total_liab_hldr_eqy（负债及股东权益总计）、oth_comp_income（其他综合收益）、contract_assets（合同资产）、contract_liab（合同负债）、use_right_assets（使用权资产）、lease_liab（租赁负债）、update_flag |

#### 2.2.3 现金流量表

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `cashflow` | 上市公司现金流量表 | ts_code、ann_date、start_date、end_date、period、report_type、comp_type、is_calc | ts_code、ann_date、f_ann_date、end_date、comp_type、report_type、net_profit（净利润）、finan_exp（财务费用）、c_fr_sale_sg（销售商品、提供劳务收到的现金）、recp_tax_rends（收到的税费返还）、c_inf_fr_operate_a（经营活动现金流入小计）、c_paid_goods_s（购买商品、接受劳务支付的现金）、c_paid_to_for_empl（支付给职工以及为职工支付的现金）、c_paid_for_taxes（支付的各项税费）、st_cash_out_act（经营活动现金流出小计）、n_cashflow_act（经营活动产生的现金流量净额）、c_disp_withdrwl_invest（收回投资收到的现金）、c_recp_return_invest（取得投资收益收到的现金）、stot_inflows_inv_act（投资活动现金流入小计）、c_pay_acq_const_fiolta（购建固定资产、无形资产和其他长期资产支付的现金）、c_paid_invest（投资支付的现金）、stot_out_inv_act（投资活动现金流出小计）、n_cashflow_inv_act（投资活动产生的现金流量净额）、c_recp_borrow（取得借款收到的现金）、proc_issue_bonds（发行债券收到的现金）、stot_cash_in_fnc_act（筹资活动现金流入小计）、c_prepay_amt_borr（偿还债务支付的现金）、c_pay_dist_dpcp_int_exp（分配股利、利润或偿付利息支付的现金）、stot_cashout_fnc_act（筹资活动现金流出小计）、n_cash_flows_fnc_act（筹资活动产生的现金流量净额）、eff_fx_flu_cash（汇率变动对现金的影响）、n_incr_cash_cash_equ（现金及现金等价物净增加额）、c_cash_equ_beg_period（期初现金及现金等价物余额）、c_cash_equ_end_period（期末现金及现金等价物余额）、free_cashflow（企业自由现金流量）、prov_depr_assets（资产减值准备）、depr_fa_coga_dpba（固定资产折旧）、amort_intang_assets（无形资产摊销）、im_net_cashflow_oper_act（经营活动产生的现金流量净额/间接法）、update_flag |

#### 2.2.4 财务指标数据

| 接口名 | 描述 | 输入参数 | 主要返回字段 |
|--------|------|---------|-------------|
| `fina_indicator` | 上市公司财务指标数据 | ts_code、ann_date、start_date、end_date、period | ts_code、ann_date、end_date（报告期）、eps（基本每股收益）、dt_eps（稀释每股收益）、total_revenue_ps（每股营业总收入）、revenue_ps（每股营业收入）、bps（每股净资产）、ocfps（每股经营活动产生的现金流量净额）、cfps（每股现金流量净额）、ebit_ps（每股息税前利润）、fcff_ps（每股企业自由现金流量）、fcfe_ps（每股股东自由现金流量）、current_ratio（流动比率）、quick_ratio（速动比率）、cash_ratio（保守速动比率）、inv_turn（存货周转率）、ar_turn（应收账款周转率）、ca_turn（流动资产周转率）、fa_turn（固定资产周转率）、assets_turn（总资产周转率）、ebit（息税前利润）、ebitda（息税折旧摊销前利润）、fcff（企业自由现金流量）、fcfe（股权自由现金流量）、netprofit_margin（销售净利率）、grossprofit_margin（销售毛利率）、cogs_of_sales（销售成本率）、expense_of_sales（销售期间费用率）、roe（净资产收益率）、roe_waa（加权平均净资产收益率）、roe_dt（净资产收益率/扣除非经常损益）、roa（总资产报酬率）、npta（总资产净利润）、roic（投入资本回报率）、debt_to_assets（资产负债率）、assets_to_eqt（权益乘数）、currentdebt_to_debt（流动负债/负债合计）、longdeb_to_debt（非流动负债/负债合计）、debt_to_eqt（产权比率）、eqt_to_debt（归属于母公司的股东权益/负债合计）、turn_days（营业周期）、rd_exp（研发费用）、basic_eps_yoy（基本每股收益同比增长率%）、dt_eps_yoy（稀释每股收益同比增长率%）、op_yoy（营业利润同比增长率%）、ebt_yoy（利润总额同比增长率%）、netprofit_yoy（归属母公司股东的净利润同比增长率%）、dt_netprofit_yoy（扣非净利润同比增长率%）、ocf_yoy（经营活动产生的现金流量净额同比增长率%）、roe_yoy（净资产收益率同比增长率%）、assets_yoy（资产总计相对年初增长率%）、eqt_yoy（归属母公司的股东权益相对年初增长率%）、tr_yoy（营业总收入同比增长率%）、or_yoy（营业收入同比增长率%）、equity_yoy（净资产同比增长率%）、update_flag |

> 提示：`fina_indicator` 现阶段每次请求最多返回 100 条记录，可通过设置日期多次请求获取更多数据。如需获取某一季度全部上市公司数据，需使用 `fina_indicator_vip` 接口（5000 积分）。

---

## 三、对比速查

### 3.1 实时行情对比

| 能力 | AKShare | Tushare Pro |
|------|---------|-------------|
| 全市场 A 股实时行情 | `stock_zh_a_spot_em`（无限制） | 无实时行情接口，需通过 `daily` 获取日终数据 |
| 个股实时行情 | `stock_bid_ask_em`、`stock_individual_spot_xq` | 不支持 |
| 五档盘口 | `stock_bid_ask_em` | 不支持 |
| 资金流向 | 部分接口支持 | `moneyflow`（2000积分起） |

### 3.2 历史行情对比

| 能力 | AKShare | Tushare Pro |
|------|---------|-------------|
| 日线行情 | `stock_zh_a_hist`（东财，无限制） | `daily`（基础积分，500次/分钟） |
| 周线/月线 | `stock_zh_a_hist`(period=weekly/monthly) | `weekly` / `monthly` |
| 复权数据 | 支持前复权/后复权 | `pro_bar` 支持前复权/后复权 |
| 分钟数据 | `stock_zh_a_hist_min_em`（1/5/15/30/60分钟） | 不支持 |
| 分时明细 | `stock_intraday_em` | 不支持 |

### 3.3 公司基本面数据对比

| 能力 | AKShare | Tushare Pro |
|------|---------|-------------|
| 股票基础信息 | `stock_individual_info_em`（东财） | `stock_basic`（2000积分起） |
| 公司概况 | `stock_individual_basic_info_xq`（雪球，详细） | 无单独接口，需从 `stock_basic` 获取 |
| 主营业务 | `stock_zyjs_ths`、`stock_zygc_em` | `fina_mainbz` |
| 利润表 | `stock_yjbb_em`（业绩报表，字段有限） | `income`（完整利润表，2000积分起） |
| 资产负债表 | 间接获取 | `balancesheet`（完整，2000积分起） |
| 现金流量表 | 间接获取 | `cashflow`（完整，2000积分起） |
| 财务指标 | 间接获取 | `fina_indicator`（约 150+ 指标，2000积分起） |
| 每日估值指标 | `stock_zh_a_spot_em` 中携带 PE/PB | `daily_basic`（每日更新，2000积分起） |

### 3.4 使用建议

| 场景 | 推荐方案 |
|------|---------|
| 实时行情监控、盘前盘后数据 | **AKShare**（`stock_zh_a_spot_em`、`stock_bid_ask_em`） |
| 历史 K 线数据批量下载 | **AKShare**（`stock_zh_a_hist`，无频率限制）或 **Tushare** `pro_bar` |
| 分钟级/分时数据 | **AKShare**（`stock_zh_a_hist_min_em`、`stock_intraday_em`） |
| 完整财务报表（利润表/资产负债表/现金流量表） | **Tushare Pro**（`income`、`balancesheet`、`cashflow`，数据规范、字段完整） |
| 财务指标分析（ROE/周转率/偿债能力等） | **Tushare Pro**（`fina_indicator`，150+ 指标，支持同比环比） |
| 每日估值指标批量获取 | **Tushare Pro**（`daily_basic`） |
| 公司基本信息与主营业务 | **AKShare**（`stock_individual_basic_info_xq` 信息详尽） + **Tushare** `stock_basic` 互补 |
| 资金流向分析 | **Tushare Pro**（`moneyflow`，大单小单分类清晰） |

---

## 四、专题：个股基础信息接口详解

> 以下专门整理 AKShare 与 Tushare Pro 在「个股基础信息/公司概况」方面的获取接口，供 collector 模块在构建股票元数据表时参考。

### 4.1 AKShare 个股基础信息接口

#### 4.1.1 `stock_individual_info_em` — 东方财富个股信息

| 项目 | 说明 |
|------|------|
| **数据源** | 东方财富 |
| **输入参数** | `symbol`: 股票代码，如 `"000001"` |
| **返回格式** | DataFrame（item / value 两列） |
| **特点** | 单次返回指定股票的核心市场信息，包含实时估值数据 |

**返回字段明细（实测 000001.SZ 平安银行）**

| 字段名（item） | 说明 | 示例值 |
|---------------|------|--------|
| `最新` | 最新收盘价 | `11.23` |
| `股票代码` | 股票代码 | `000001` |
| `股票简称` | 股票简称 | `平安银行` |
| `总股本` | 总股本（股） | `19405918198.0` |
| `流通股` | 流通股本（股） | `19405600653.0` |
| `总市值` | 总市值（元） | `217928461363.54` |
| `流通市值` | 流通市值（元） | `217924895333.19` |
| `行业` | 所属行业 | `银行Ⅱ` |
| `上市时间` | 上市日期 | `19910403` |

> ⚠️ 注意：该接口返回的是 **item / value 键值对**，不是标准列式 DataFrame。如需列式结构，需自行 pivot。

---

#### 4.1.2 `stock_profile_cninfo` — 巨潮资讯个股资料

| 项目 | 说明 |
|------|------|
| **数据源** | 巨潮资讯网 |
| **输入参数** | `symbol`: 股票代码，如 `"600519"` |
| **返回格式** | 标准列式 DataFrame（1 行 × 26 列） |
| **特点** | 信息最全面，包含公司沿革、联系方式、注册信息、主营业务等 |

**返回字段明细（实测 600519.SH 贵州茅台）**

| 字段名 | 说明 | 示例值 |
|--------|------|--------|
| `公司名称` | 公司全称 | `贵州茅台酒股份有限公司` |
| `英文名称` | 英文全称 | `Kweichow Moutai Co., Ltd.` |
| `曾用简称` | 历史简称 | `None` |
| `A股代码` | A股代码 | `600519` |
| `A股简称` | A股简称 | `贵州茅台` |
| `B股代码` | B股代码 | `None` |
| `B股简称` | B股简称 | `None` |
| `H股代码` | H股代码 | `None` |
| `H股简称` | H股简称 | `None` |
| `入选指数` | 纳入的指数成分 | `ESG 300, 沪深300, 上证50, 中证A100, ...` |
| `所属市场` | 交易所 | `上交所` |
| `所属行业` | 证监会行业分类 | `酒、饮料和精制茶制造业` |
| `法人代表` | 法定代表人 | `陈华` |
| `注册资金` | 注册资本（万元） | `125227.0215` |
| `成立日期` | 公司成立日期 | `1999-11-20` |
| `上市日期` | 股票上市日期 | `2001-08-27` |
| `官方网站` | 官网 | `www.moutaichina.com` |
| `电子邮箱` | 联系邮箱 | `mtdm@moutaichina.com` |
| `联系电话` | 联系电话 | `0851-22386002` |
| `传真` | 传真 | `0851-22386193` |
| `注册地址` | 注册地址 | `贵州省仁怀市茅台镇` |
| `办公地址` | 办公地址 | `贵州省仁怀市茅台镇` |
| `邮政编码` | 邮编 | `564501` |
| `主营业务` | 主营业务描述 | `贵州茅台酒系列产品的产品研制、酿造生产、包装和销售。` |
| `经营范围` | 经营范围 | `茅台酒系列产品的生产与销售；饮料、食品、包装材料的生产、销售；...` |
| `机构简介` | 公司沿革简介 | `公司于1999年11月20日，由...发起设立。...` |

> ✅ **推荐**：若需最完整的公司静态信息，优先使用此接口。

---

#### 4.1.3 `stock_zyjs_ths` — 同花顺主营介绍

| 项目 | 说明 |
|------|------|
| **数据源** | 同花顺 |
| **输入参数** | `symbol`: 股票代码，如 `"000001"` |
| **返回格式** | 标准列式 DataFrame（1 行 × 5 列） |
| **特点** | 聚焦主营业务与产品信息 |

**返回字段明细**

| 字段名 | 说明 | 示例值 |
|--------|------|--------|
| `股票代码` | 股票代码 | `000001` |
| `主营业务` | 主营业务概述 | `经有关监管机构批准的各项商业银行业务。` |
| `产品类型` | 产品类型 | `商业银行业务` |
| `产品名称` | 产品名称 | `商业银行业务` |
| `经营范围` | 经营范围 | `办理人民币存、贷、结算、汇兑业务；...` |

---

### 4.2 Tushare Pro 个股基础信息接口

#### 4.2.1 `stock_basic` — 股票基础信息

| 项目 | 说明 |
|------|------|
| **数据源** | Tushare 自采 |
| **输入参数** | `exchange`, `list_status`, `market`, `is_hs`, `name`, `ts_code`, `fields` |
| **返回格式** | 标准列式 DataFrame |
| **特点** | 覆盖全市场，一次可拉取全部股票列表；含实控人信息；支持字段筛选 |
| **权限** | 2000 积分起，每分钟 50 次 |

**返回字段明细**

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `ts_code` | str | TS 代码（带后缀） | `000001.SZ` |
| `symbol` | str | 股票代码 | `000001` |
| `name` | str | 股票名称 | `平安银行` |
| `area` | str | 地域 | `深圳` |
| `industry` | str | 所属行业 | `银行` |
| `fullname` | str | 股票全称 | `平安银行股份有限公司` |
| `enname` | str | 英文全称 | `Ping An Bank Co., Ltd.` |
| `cnspell` | str | 拼音缩写 | `PAYH` |
| `market` | str | 市场类型 | `主板` / `创业板` / `科创板` / `CDR` / `北交所` |
| `exchange` | str | 交易所代码 | `SZSE` / `SSE` / `BSE` |
| `curr_type` | str | 交易货币 | `CNY` |
| `list_status` | str | 上市状态 | `L`上市 / `D`退市 / `P`暂停上市 / `G`未交易 |
| `list_date` | str | 上市日期 | `19910403` |
| `delist_date` | str | 退市日期 | `None` |
| `is_hs` | str | 是否沪深港通标的 | `N`否 / `H`沪股通 / `S`深股通 |
| `act_name` | str | 实控人名称 | `无实际控制人` |
| `act_ent_type` | str | 实控人企业性质 | `其他` / `民营企业` / `国有企业` / `外资企业` |

> 💡 **使用建议**：建议在系统初始化时全量拉取一次并持久化到本地数据库，后续增量更新即可。字段 `list_status` 可用于过滤退市/暂停上市股票。

---

### 4.3 个股基础信息字段对比

| 信息维度 | AKShare `stock_individual_info_em` | AKShare `stock_profile_cninfo` | AKShare `stock_zyjs_ths` | Tushare `stock_basic` |
|---------|-----------------------------------|-------------------------------|-------------------------|----------------------|
| 股票代码 | ✅ | ✅ | ✅ | ✅ |
| 股票简称 | ✅ | ✅ | — | ✅ |
| 公司全称 | — | ✅ | — | ✅ |
| 英文名称 | — | ✅ | — | ✅ |
| 所属行业 | ✅ | ✅ | — | ✅ |
| 地域/区域 | — | — | — | ✅ |
| 上市日期 | ✅ | ✅ | — | ✅ |
| 退市日期 | — | — | — | ✅ |
| 成立日期 | — | ✅ | — | — |
| 注册资本 | — | ✅ | — | — |
| 法人代表 | — | ✅ | — | — |
| 实控人 | — | — | — | ✅ |
| 企业性质 | — | — | — | ✅ |
| 总股本 | ✅ | — | — | — |
| 流通股本 | ✅ | — | — | — |
| 总市值 | ✅ | — | — | — |
| 流通市值 | ✅ | — | — | — |
| 最新价 | ✅ | — | — | — |
| 主营业务 | — | ✅ | ✅ | — |
| 经营范围 | — | ✅ | ✅ | — |
| 产品类型/名称 | — | — | ✅ | — |
| 联系方式 | — | ✅ | — | — |
| 注册/办公地址 | — | ✅ | — | — |
| 公司沿革 | — | ✅ | — | — |
| 入选指数 | — | ✅ | — | — |
| A/B/H股标识 | — | ✅ | — | — |
| 沪深港通标的 | — | — | — | ✅ |
| 上市状态 | — | — | — | ✅ |

---

### 4.4 采集建议（针对本项目 collector）

| 目标表/场景 | 推荐接口 | 说明 |
|------------|---------|------|
| **股票主表（tb_stock_basic）** | AKShare `stock_info_a_code_name` + `stock_info_sz_name_code` | 总股本、流通股本、行业、市场类型 |
| **公司静态信息表（tb_company_basic）** | AKShare `stock_profile_cninfo` | 信息最全面，含法人代表、地址、沿革、主营业务 |
| **主营业务表（business）** | AKShare `stock_zyjs_ths` | 结构化程度高，直接对应产品维度 |
| **实时估值快照** | AKShare `stock_individual_info_em` | 携带最新价、市值等实时数据 |
| **每日指标更新** | Tushare `daily_basic` | PE/PB/市值等每日指标批量获取 |

---

> 附录：
> - AKShare 官方文档：https://akshare.akfamily.xyz/
> - Tushare Pro 官方文档：https://www.tushare.pro/document/2
