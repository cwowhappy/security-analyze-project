# 公司信息模块 — 前后端接口契约

## 概述

本文档定义公司信息模块的前后端 RESTful API 接口契约，作为双方开发的共同依据。接口变更时需同步更新本文档。

**基地址**：`http://localhost:8080`
**版本前缀**：`/api`
**内容类型**：`application/json`

---

## 通用结构

### 分页请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| keyword | string | 否 | — | 搜索关键词；股票代码精确匹配，公司名称前缀匹配（大小写不敏感） |
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

| 字段 | 类型 | 说明 |
|------|------|------|
| items | array | 当前页数据列表 |
| total | integer | 总记录数 |
| page | integer | 当前页码 |
| size | integer | 每页条数 |

### 错误响应结构

```json
{
  "code": 400,
  "message": "错误描述"
}
```

---

## 接口清单

### 1. 公司列表

**GET** `/api/companies`

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| keyword | string | 否 | — | 股票代码精确匹配，公司名称前缀匹配（大小写不敏感） |
| page | integer | 否 | 0 | 页码 |
| size | integer | 否 | 20 | 每页条数 |

#### 请求示例

```
GET /api/companies?keyword=茅台&page=0&size=20
```

#### 响应结构

```json
{
  "items": [
    {
      "stockCode": "600519",
      "stockName": "贵州茅台",
      "industry": "白酒",
      "region": "贵州",
      "listingDate": "2001-08-27",
      "market": "SH"
    }
  ],
  "total": 1,
  "page": 0,
  "size": 20
}
```

#### 列表项字段

| 字段 | 类型 | 说明 |
|------|------|------|
| stockCode | string | 股票代码 |
| stockName | string | 公司简称 |
| industry | string | 所属行业 |
| region | string | 所属地区 |
| listingDate | string (date) | 上市日期，格式 `YYYY-MM-DD` |
| market | string | 市场板块：SH / SZ / BJ |

#### 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 500 | 服务器内部错误 |

---

### 2. 公司详情

**GET** `/api/companies/{stockCode}`

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| stockCode | string | 是 | 股票代码 |

#### 请求示例

```
GET /api/companies/600519
```

#### 响应结构

```json
{
  "stockCode": "600519",
  "stockName": "贵州茅台",
  "industry": "白酒",
  "region": "贵州",
  "establishDate": "1999-11-20",
  "registeredCapital": 125619.7800,
  "listingDate": "2001-08-27",
  "market": "SH"
}
```

#### 响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| stockCode | string | 股票代码 |
| stockName | string | 公司简称 |
| industry | string | 所属行业 |
| region | string | 所属地区（省份/城市） |
| establishDate | string (date) | 成立日期，格式 `YYYY-MM-DD` |
| registeredCapital | number | 注册资本（万元） |
| listingDate | string (date) | 上市日期，格式 `YYYY-MM-DD` |
| market | string | 市场板块：SH / SZ / BJ |

#### 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 404 | 公司不存在 |
| 500 | 服务器内部错误 |

---

## DTO 字段约束

### CompanyListItem

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| stockCode | String | string | 非空，长度 ≤ 20 |
| stockName | String | string | 非空，长度 ≤ 100 |
| industry | String | string | 可空，长度 ≤ 100 |
| region | String | string | 可空，长度 ≤ 50 |
| listingDate | LocalDate | string | 可空，格式 `YYYY-MM-DD` |
| market | String | string | 可空，枚举：SH / SZ / BJ |

### CompanyDetail

| 字段 | Java 类型 | TypeScript 类型 | 约束 |
|------|-----------|-----------------|------|
| stockCode | String | string | 非空，长度 ≤ 20 |
| stockName | String | string | 非空，长度 ≤ 100 |
| industry | String | string | 可空，长度 ≤ 100 |
| region | String | string | 可空，长度 ≤ 50 |
| establishDate | LocalDate | string | 可空，格式 `YYYY-MM-DD` |
| registeredCapital | BigDecimal | number | 可空，精度 20,4 |
| listingDate | LocalDate | string | 可空，格式 `YYYY-MM-DD` |
| market | String | string | 可空，枚举：SH / SZ / BJ |

---

## 修订记录

| 日期 | 版本 | 说明 | 作者 |
|------|------|------|------|
| 2026-04-30 | v1.0 | 初始版本，定义公司列表与详情接口 | — |
| 2026-04-30 | v1.1 | 搜索规则调整：stock_code 改为精确匹配，stock_name 改为前缀匹配 | — |
