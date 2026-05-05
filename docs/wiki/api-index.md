# 指数模块 — 前后端接口契约

## 概述

本文档定义指数模块的前后端 RESTful API 接口契约。接口变更时需同步更新本文档。

**基地址**：`http://localhost:8080`  
**版本前缀**：`/api`  
**内容类型**：`application/json`

---

## 通用结构

### 分页请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| keyword | string | 否 | — | 搜索关键词；指数代码精确匹配，指数名称前缀匹配（大小写不敏感） |
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

### 1. 指数列表

**GET** `/api/indexes`

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| keyword | string | 否 | — | 指数代码精确匹配，指数名称模糊匹配 |
| page | integer | 否 | 0 | 页码 |
| size | integer | 否 | 20 | 每页条数 |

#### 响应结构

```json
{
  "items": [
    {
      "indexCode": "000001",
      "indexName": "上证指数",
      "indexType": "宽基",
      "market": "SH",
      "publishDate": "1991-07-15"
    }
  ],
  "total": 726,
  "page": 0,
  "size": 20
}
```

---

### 2. 指数详情

**GET** `/api/indexes/{indexCode}`

#### 响应结构

```json
{
  "indexCode": "000001",
  "indexName": "上证指数",
  "indexType": "宽基",
  "market": "SH",
  "baseDate": "1990-12-19",
  "basePoint": 100.0000,
  "componentCount": null,
  "publishDate": "1991-07-15"
}
```

---

### 3. 指数趋势

**GET** `/api/indexes/{indexCode}/trend`

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| granularity | string | 否 | day | 粒度：day / week / month |
| startDate | string (date) | 否 | — | 起始日期，格式 `YYYY-MM-DD` |
| endDate | string (date) | 否 | — | 结束日期，格式 `YYYY-MM-DD` |

#### 响应结构

```json
{
  "indexCode": "000001",
  "granularity": "day",
  "items": [
    {
      "tradeDate": "2024-01-02",
      "openPrice": 2972.78,
      "highPrice": 2976.27,
      "lowPrice": 2962.28,
      "closePrice": 2962.28,
      "volume": 304141793,
      "amount": 345950700000.0,
      "amplitude": 0.47,
      "changePct": -0.43,
      "changeAmount": -12.65,
      "turnoverRate": 0.63
    }
  ]
}
```

---

### 4. 分类核心指数

**GET** `/api/indexes/categories`

返回按类型分组的核心指数列表（`is_core = TRUE`）。

#### 响应结构

```json
[
  {
    "indexType": "宽基",
    "indexTypeLabel": "宽基指数",
    "items": [
      {
        "indexCode": "000001",
        "indexName": "上证指数",
        "indexType": "宽基",
        "market": "SH",
        "publishDate": "1991-07-15"
      }
    ]
  }
]
```

---

### 5. 指数关联 ETF

**GET** `/api/indexes/{indexCode}/etfs`

#### 响应结构

```json
[
  {
    "etfCode": "510300",
    "etfName": "华泰柏瑞沪深300ETF",
    "trackingIndexCode": "000300",
    "managementFee": null,
    "fundSize": 1200000000.0,
    "establishDate": "2012-05-04",
    "market": "SH"
  }
]
```

---

## DTO 字段约束

### IndexListItem

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| indexCode | String | string | 非空，长度 ≤ 20 |
| indexName | String | string | 非空，长度 ≤ 200 |
| indexType | String | string | 可空，长度 ≤ 50 |
| market | String | string | 可空，枚举：SH / SZ / BJ / HK / US / CN |
| publishDate | LocalDate | string | 可空，格式 `YYYY-MM-DD` |

### IndexDetailResponse

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| indexCode | String | string | 非空 |
| indexName | String | string | 非空 |
| indexType | String | string | 可空 |
| market | String | string | 可空 |
| baseDate | LocalDate | string | 可空 |
| basePoint | BigDecimal | number | 可空 |
| componentCount | Integer | number | 可空 |
| publishDate | LocalDate | string | 可空 |

### IndexTrendItem

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| tradeDate | LocalDate | string | 非空 |
| openPrice | BigDecimal | number | 可空 |
| highPrice | BigDecimal | number | 可空 |
| lowPrice | BigDecimal | number | 可空 |
| closePrice | BigDecimal | number | 可空 |
| volume | Long | number | 可空 |
| amount | BigDecimal | number | 可空 |
| amplitude | BigDecimal | number | 可空 |
| changePct | BigDecimal | number | 可空 |
| changeAmount | BigDecimal | number | 可空 |
| turnoverRate | BigDecimal | number | 可空 |

### EtfListItem

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| etfCode | String | string | 非空 |
| etfName | String | string | 非空 |
| trackingIndexCode | String | string | 可空 |
| managementFee | BigDecimal | number | 可空 |
| fundSize | BigDecimal | number | 可空 |
| establishDate | LocalDate | string | 可空 |
| market | String | string | 可空 |

---

## 修订记录

| 日期 | 版本 | 说明 | 作者 |
|------|------|------|------|
| 2026-05-05 | v1.0 | 初始版本，定义指数列表、详情、趋势、关联 ETF 接口 | — |
