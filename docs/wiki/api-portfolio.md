# 持仓管理模块 — 前后端接口契约

## 概述

本文档定义持仓管理模块的前后端 RESTful API 接口契约。接口变更时需同步更新本文档。

**基地址**：`http://localhost:8080`  
**版本前缀**：`/api`  
**内容类型**：`application/json`

所有接口（除公开说明外）均需要 JWT 认证，且只能访问当前用户所属的 portfolio 数据。

---

## 通用结构

### 分页请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| page | integer | 否 | 0 | 页码，从 0 开始 |
| size | integer | 否 | 20 | 每页条数，最大 100 |

### 分页响应结构

```json
{
  "items": [],
  "total": 0,
  "page": 0,
  "size": 20
}
```

---

## 接口清单

### 组合管理

#### 1. 获取当前用户的组合列表

**GET** `/api/portfolios`

##### 响应结构

```json
[
  {
    "id": 1,
    "name": "华泰证券主账户",
    "type": "REAL",
    "broker": "华泰证券",
    "description": "A股实盘账户",
    "createdAt": "2026-01-15T10:30:00"
  }
]
```

---

#### 2. 创建组合

**POST** `/api/portfolios`

##### 请求体

```json
{
  "name": "华泰证券主账户",
  "type": "REAL",
  "broker": "华泰证券",
  "description": "A股实盘账户"
}
```

##### 字段约束

| 字段 | 类型 | 必填 | 约束 |
|------|------|:----:|------|
| name | string | 是 | 长度 1-100 |
| type | string | 是 | 枚举：REAL / SIMULATION |
| broker | string | 否 | 长度 0-100 |
| description | string | 否 | 长度 0-500 |

##### 响应结构

```json
{
  "id": 1,
  "name": "华泰证券主账户",
  "type": "REAL",
  "broker": "华泰证券",
  "description": "A股实盘账户",
  "createdAt": "2026-05-06T14:00:00"
}
```

---

#### 3. 修改组合信息

**PUT** `/api/portfolios/{id}`

##### 请求体

同创建组合，但不允许修改 `type`（或后端忽略该字段）。

##### 响应结构

同创建组合响应。

---

#### 4. 删除组合

**DELETE** `/api/portfolios/{id}`

级联逻辑删除该组合下的所有成交记录和持仓记录（`is_deleted = TRUE`，非物理删除）。

##### 响应

`204 No Content`

---

### 成交记录

#### 5. 分页查询成交记录

**GET** `/api/portfolios/{portfolioId}/transactions`

##### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| stockCode | string | 否 | — | 按股票代码筛选 |
| tradeType | string | 否 | — | 按成交类型筛选 |
| startDate | string (date) | 否 | — | 起始日期，格式 `YYYY-MM-DD` |
| endDate | string (date) | 否 | — | 结束日期，格式 `YYYY-MM-DD` |
| page | integer | 否 | 0 | 页码 |
| size | integer | 否 | 20 | 每页条数 |

##### 响应结构

```json
{
  "items": [
    {
      "id": 1,
      "portfolioId": 1,
      "stockCode": "600519",
      "stockName": "贵州茅台",
      "tradeDate": "2026-05-05",
      "tradeType": "BUY",
      "tradeTypeLabel": "买入",
      "price": 1688.00,
      "quantity": 100,
      "fee": 5.00,
      "tax": 0,
      "amount": 168800.00,
      "realizedPnl": 0,
      "remark": "茅台建仓",
      "createdAt": "2026-05-06T14:00:00"
    }
  ],
  "total": 1,
  "page": 0,
  "size": 20
}
```

---

#### 6. 录入成交

**POST** `/api/portfolios/{portfolioId}/transactions`

##### 请求体（买入示例）

```json
{
  "stockCode": "600519",
  "tradeDate": "2026-05-05",
  "tradeType": "BUY",
  "price": 1688.00,
  "quantity": 100,
  "fee": 5.00,
  "tax": 0,
  "remark": "茅台建仓"
}
```

##### 请求体（分红示例）

```json
{
  "stockCode": "600519",
  "tradeDate": "2026-06-15",
  "tradeType": "DIVIDEND",
  "price": null,
  "quantity": 0,
  "fee": 0,
  "tax": 25.00,
  "remark": "每股分红2.5元，扣税10%"
}
```

##### 字段约束

| 字段 | 类型 | 必填 | 约束 |
|------|------|:----:|------|
| stockCode | string | 是 | 长度 1-20，须为有效 A 股代码 |
| tradeDate | string (date) | 是 | 格式 `YYYY-MM-DD`，不可晚于今天 |
| tradeType | string | 是 | 枚举：BUY / SELL / DIVIDEND / BONUS / RIGHTS / SPLIT / MERGER / OTHER |
| price | number | 条件 | BUY/SELL/RIGHTS 必填，DIVIDEND/BONUS 可空 |
| quantity | number | 是 | ≥ 0；DIVIDEND 可为 0 |
| fee | number | 否 | ≥ 0，默认 0 |
| tax | number | 否 | ≥ 0，默认 0 |
| remark | string | 否 | 长度 0-500 |

**服务端自动计算**：
- `amount` = `price` * `quantity`（若 price 为空则 amount 为 null）
- 录入成功后触发该 `portfolioId + stockCode` 的持仓重算

##### 响应结构

```json
{
  "id": 1,
  "portfolioId": 1,
  "stockCode": "600519",
  "tradeDate": "2026-05-05",
  "tradeType": "BUY",
  "price": 1688.00,
  "quantity": 100,
  "fee": 5.00,
  "tax": 0,
  "amount": 168800.00,
  "realizedPnl": 0,
  "remark": "茅台建仓",
  "createdAt": "2026-05-06T14:00:00"
}
```

---

#### 7. 修改成交

**PUT** `/api/transactions/{id}`

修改成交记录后，触发对应持仓重算。

##### 请求体

同录入交易，不含 `portfolioId`。

##### 响应结构

同录入交易响应。

---

#### 8. 删除成交

**DELETE** `/api/transactions/{id}`

逻辑删除成交记录（`is_deleted = TRUE`，非物理删除），删除后触发对应持仓重算。

##### 响应

`204 No Content`

---

### 持仓概览

#### 9. 当前持仓列表（含行情）

**GET** `/api/portfolios/{portfolioId}/positions`

返回当前持仓股数 > 0 的股票，按持仓市值降序排列。

##### 响应结构

```json
[
  {
    "stockCode": "600519",
    "stockName": "贵州茅台",
    "industry": "白酒",
    "market": "SH",
    "currentQuantity": 100,
    "avgCost": 1688.00,
    "closePrice": 1750.00,
    "marketValue": 175000.00,
    "totalCost": 168800.00,
    "floatingPnl": 6200.00,
    "floatingPnlRate": 3.67,
    "realizedPnl": 0,
    "firstBuyDate": "2026-05-05",
    "lastTradeDate": "2026-05-05",
    "weight": 45.2
  }
]
```

##### 字段说明

| 字段 | 说明 |
|------|------|
| closePrice | 最近交易日收盘价（来自 daily_quote）；若当日无数据，取最近一次有效收盘价 |
| marketValue | `currentQuantity * closePrice` |
| floatingPnl | `currentQuantity * (closePrice - avgCost)` |
| floatingPnlRate | `(closePrice - avgCost) / avgCost * 100`；avgCost 为 0 时返回 0 |
| weight | 该持仓市值占组合总市值的百分比（仓位占比）；组合总市值为 0 时返回 0 |

---

#### 10. 组合总体统计

**GET** `/api/portfolios/{portfolioId}/summary`

##### 响应结构

```json
{
  "portfolioId": 1,
  "portfolioName": "华泰证券主账户",
  "totalMarketValue": 387200.00,
  "totalCost": 365000.00,
  "totalFloatingPnl": 22200.00,
  "totalFloatingPnlRate": 6.08,
  "totalRealizedPnl": 5000.00,
  "totalAssetReturn": 27200.00,
  "totalAssetReturnRate": 7.45,
  "holdingCount": 3,
  "latestTradeDate": "2026-05-05"
}
```

##### 字段说明

| 字段 | 说明 |
|------|------|
| totalMarketValue | 组合当前总市值（各持仓 marketValue 之和） |
| totalCost | 组合总成本（各持仓 total_cost 之和） |
| totalFloatingPnl | 总浮动盈亏 |
| totalFloatingPnlRate | 总浮动盈亏率 = `totalFloatingPnl / totalCost * 100` |
| totalRealizedPnl | 总已实现盈亏（清仓股票的历史收益 + 持仓中的已实现部分） |
| totalAssetReturn | 累计盈亏 = `totalFloatingPnl + totalRealizedPnl` |
| totalAssetReturnRate | 累计收益率（首期简化为 `totalAssetReturn / totalCost * 100`） |
| holdingCount | 当前持仓股票数 |
| latestTradeDate | 最近一笔交易的日期 |

---

## DTO 字段约束

### PortfolioRequest

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| name | String | string | 非空，长度 1-100 |
| type | PortfolioType | string | 非空，枚举：REAL / SIMULATION |
| broker | String | string | 可空，长度 0-100 |
| description | String | string | 可空，长度 0-500 |

### PortfolioResponse

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| id | Long | number | 非空 |
| name | String | string | 非空 |
| type | PortfolioType | string | 非空 |
| broker | String | string | 可空 |
| description | String | string | 可空 |
| createdAt | LocalDateTime | string | 非空 |

### TransactionRequest

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| stockCode | String | string | 非空，长度 1-20 |
| tradeDate | LocalDate | string | 非空，格式 `YYYY-MM-DD` |
| tradeType | TradeType | string | 非空 |
| price | BigDecimal | number | BUY/SELL/RIGHTS 必填 |
| quantity | BigDecimal | number | 非空，≥ 0 |
| fee | BigDecimal | number | 可空，≥ 0，默认 0 |
| tax | BigDecimal | number | 可空，≥ 0，默认 0 |
| remark | String | string | 可空，长度 0-500 |

### TransactionResponse

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| id | Long | number | 非空 |
| portfolioId | Long | number | 非空 |
| stockCode | String | string | 非空 |
| stockName | String | string | 可空（后端关联 company 表填充） |
| tradeDate | LocalDate | string | 非空 |
| tradeType | TradeType | string | 非空 |
| tradeTypeLabel | String | string | 非空，中文展示（如"买入"） |
| price | BigDecimal | number | 可空 |
| quantity | BigDecimal | number | 非空 |
| fee | BigDecimal | number | 非空 |
| tax | BigDecimal | number | 非空 |
| amount | BigDecimal | number | 可空 |
| realizedPnl | BigDecimal | number | 非空 |
| remark | String | string | 可空 |
| createdAt | LocalDateTime | string | 非空 |

### PositionResponse

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| stockCode | String | string | 非空 |
| stockName | String | string | 可空 |
| industry | String | string | 可空 |
| market | String | string | 可空 |
| currentQuantity | BigDecimal | number | 非空 |
| avgCost | BigDecimal | number | 非空 |
| closePrice | BigDecimal | number | 可空 |
| marketValue | BigDecimal | number | 可空 |
| totalCost | BigDecimal | number | 非空 |
| floatingPnl | BigDecimal | number | 可空 |
| floatingPnlRate | BigDecimal | number | 可空 |
| realizedPnl | BigDecimal | number | 非空 |
| firstBuyDate | LocalDate | string | 可空 |
| lastTradeDate | LocalDate | string | 可空 |
| weight | BigDecimal | number | 可空 |

### PortfolioSummaryResponse

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| portfolioId | Long | number | 非空 |
| portfolioName | String | string | 非空 |
| totalMarketValue | BigDecimal | number | 可空 |
| totalCost | BigDecimal | number | 可空 |
| totalFloatingPnl | BigDecimal | number | 可空 |
| totalFloatingPnlRate | BigDecimal | number | 可空 |
| totalRealizedPnl | BigDecimal | number | 可空 |
| totalAssetReturn | BigDecimal | number | 可空 |
| totalAssetReturnRate | BigDecimal | number | 可空 |
| holdingCount | Integer | number | 非空 |
| latestTradeDate | LocalDate | string | 可空 |

---

## 错误码

| HTTP 状态码 | 错误码 | 说明 |
|-------------|--------|------|
| 400 | INVALID_TRADE_TYPE | 交易类型与字段不匹配（如 BUY 未填 price） |
| 400 | INVALID_TRADE_DATE | 交易日期晚于今天 |
| 403 | PORTFOLIO_ACCESS_DENIED | 尝试访问不属于当前用户的组合 |
| 404 | PORTFOLIO_NOT_FOUND | 组合不存在 |
| 404 | TRANSACTION_NOT_FOUND | 交易记录不存在 |
| 409 | INSUFFICIENT_POSITION | 卖出股数超过当前可卖持仓 |

---

## 修订记录

| 日期 | 版本 | 说明 | 作者 |
|------|------|------|------|
| 2026-05-06 | v1.0 | 初始版本，定义组合、交易、持仓、统计接口 | — |
