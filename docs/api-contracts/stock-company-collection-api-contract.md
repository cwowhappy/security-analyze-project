# 前后端交互契约：股票与公司基础信息模块

> 本文档定义后端 REST API 的请求/响应格式、状态码与字段说明。

---

## 一、统一响应格式

所有接口返回统一包装：

```json
{
  "success": true,
  "code": 200,
  "message": "OK",
  "data": { ... },
  "timestamp": 1715340000000
}
```

**错误响应示例：**

```json
{
  "success": false,
  "code": 404,
  "message": "股票不存在: 999999",
  "data": null,
  "timestamp": 1715340000000
}
```

---

## 二、股票 API

### 2.1 获取股票列表

```
GET /api/stocks?page=1&size=20&industry=银行
```

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 20 |
| industry | string | 否 | 行业筛选 |
| market | string | 否 | 市场类型筛选 |

**响应：**

```json
{
  "success": true,
  "code": 200,
  "message": "OK",
  "data": {
    "list": [
      {
        "id": "stk_xxx",
        "stockCode": "000001",
        "tsCode": "000001.SZ",
        "name": "平安银行",
        "fullName": "平安银行股份有限公司",
        "market": "主板",
        "exchange": "SZ",
        "listDate": "1991-04-03",
        "industry": "银行",
        "area": "深圳",
        "totalShares": 19405918198,
        "floatShares": 19405562184,
        "updatedAt": "2026-05-10T10:00:00"
      }
    ],
    "total": 5200,
    "page": 1,
    "size": 20
  }
}
```

### 2.2 获取股票详情

```
GET /api/stocks/{stockCode}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| stockCode | string | 股票代码，如 000001 |

**响应：**

```json
{
  "success": true,
  "code": 200,
  "data": {
    "id": "stk_xxx",
    "stockCode": "000001",
    "tsCode": "000001.SZ",
    "name": "平安银行",
    "fullName": "平安银行股份有限公司",
    "market": "主板",
    "exchange": "SZ",
    "listDate": "1991-04-03",
    "industry": "银行",
    "area": "深圳",
    "totalShares": 19405918198,
    "floatShares": 19405562184,
    "updatedAt": "2026-05-10T10:00:00",
    "company": {
      "id": "cmp_xxx",
      "unifiedSocialCreditCode": "9144030019218538XX",
      "name": "平安银行股份有限公司",
      "legalRepresentative": "谢永林",
      "regCapital": 1940591.8198,
      "setupDate": "1987-12-22",
      "mainBusiness": "经有关监管机构批准的各项商业银行业务"
    }
  }
}
```

---

## 三、公司 API

### 3.1 获取公司列表

```
GET /api/companies?page=1&size=20&industry=银行&province=深圳
```

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 20 |
| industry | string | 否 | 行业筛选 |
| province | string | 否 | 省份筛选 |
| keyword | string | 否 | 公司名称关键词 |

**响应：**

```json
{
  "success": true,
  "code": 200,
  "data": {
    "list": [
      {
        "id": "cmp_xxx",
        "unifiedSocialCreditCode": "9144030019218538XX",
        "name": "平安银行股份有限公司",
        "shortName": "平安银行",
        "industry": "银行",
        "province": "广东省",
        "city": "深圳市",
        "setupDate": "1987-12-22",
        "regCapital": 1940591.8198,
        "employees": 44277
      }
    ],
    "total": 5200,
    "page": 1,
    "size": 20
  }
}
```

### 3.2 获取公司详情

```
GET /api/companies/{uscCode}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| uscCode | string | 统一社会信用代码 |

**响应：**

```json
{
  "success": true,
  "code": 200,
  "data": {
    "id": "cmp_xxx",
    "unifiedSocialCreditCode": "9144030019218538XX",
    "name": "平安银行股份有限公司",
    "shortName": "平安银行",
    "englishName": "Ping An Bank Co., Ltd.",
    "legalRepresentative": "谢永林",
    "chairman": "谢永林",
    "manager": "胡跃飞",
    "secretary": "周强",
    "regCapital": 1940591.8198,
    "setupDate": "1987-12-22",
    "province": "广东省",
    "city": "深圳市",
    "regAddress": "深圳市罗湖区深南东路5047号",
    "officeAddress": "深圳市福田区益田路5023号平安金融中心",
    "website": "bank.pingan.com",
    "industry": "银行",
    "mainBusiness": "经有关监管机构批准的各项商业银行业务",
    "businessScope": "吸收公众存款；发放短期、中期和长期贷款...",
    "introduction": "平安银行是一家总部设在深圳的全国性股份制商业银行",
    "employees": 44277,
    "controllerName": "中国平安保险（集团）股份有限公司",
    "controllerType": "其他",
    "stocks": [
      {
        "stockCode": "000001",
        "name": "平安银行",
        "market": "主板",
        "exchange": "SZ",
        "listDate": "1991-04-03"
      }
    ]
  }
}
```

---

## 四、采集任务 API

### 4.1 获取任务列表

```
GET /api/collection/tasks?page=1&size=20&status=success
```

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 20 |
| status | string | 否 | 状态筛选：pending/running/success/failed |
| taskType | string | 否 | 任务类型筛选 |

**响应：**

```json
{
  "success": true,
  "code": 200,
  "data": {
    "list": [
      {
        "id": "tsk_xxx",
        "taskType": "stock_full",
        "taskParams": null,
        "status": "success",
        "dataSource": "akshare",
        "totalCount": 5200,
        "successCount": 5180,
        "failCount": 20,
        "startedAt": "2026-05-10T02:00:00",
        "completedAt": "2026-05-10T02:35:00",
        "createdAt": "2026-05-10T02:00:00"
      }
    ],
    "total": 156,
    "page": 1,
    "size": 20
  }
}
```

### 4.2 创建即时采集任务

```
POST /api/collection/tasks
```

**请求体：**

```json
{
  "taskType": "stock_single",
  "taskParams": {
    "stockCode": "000001"
  },
  "dataSource": "akshare"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskType | string | 是 | stock_full / company_full / stock_single / company_single |
| taskParams | object | 否 | 任务参数，如 {"stockCode": "000001"} |
| dataSource | string | 否 | akshare / tushare，默认 akshare |

**响应：**

```json
{
  "success": true,
  "code": 200,
  "data": {
    "id": "tsk_xxx",
    "taskType": "stock_single",
    "status": "pending",
    "dataSource": "akshare",
    "createdAt": "2026-05-10T10:00:00"
  }
}
```

### 4.3 获取任务详情

```
GET /api/collection/tasks/{id}
```

**响应：** 同任务列表中的单条记录，含 `errorMessage` 字段。

---

## 五、定时规则 API

### 5.1 获取规则列表

```
GET /api/collection/schedules
```

**响应：**

```json
{
  "success": true,
  "code": 200,
  "data": [
    {
      "id": "sch_xxx",
      "name": "每日股票全量采集",
      "taskType": "stock_full",
      "dataSource": "akshare",
      "cronExpression": "0 2 * * *",
      "isEnabled": true,
      "lastTriggeredAt": "2026-05-10T02:00:00",
      "createdAt": "2026-05-01T10:00:00"
    }
  ]
}
```

### 5.2 创建定时规则

```
POST /api/collection/schedules
```

**请求体：**

```json
{
  "name": "每日公司全量采集",
  "taskType": "company_full",
  "cronExpression": "0 3 * * *",
  "dataSource": "akshare",
  "taskParams": null
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 规则名称 |
| taskType | string | 是 | 任务类型 |
| cronExpression | string | 是 | Cron 表达式 |
| dataSource | string | 否 | 默认数据源 |
| taskParams | object | 否 | 默认参数 |

### 5.3 更新定时规则

```
PUT /api/collection/schedules/{id}
```

**请求体：**

```json
{
  "name": "每日公司全量采集",
  "cronExpression": "0 4 * * *",
  "isEnabled": false
}
```

> 支持部分更新，仅传入需要修改的字段。

### 5.4 删除定时规则

```
DELETE /api/collection/schedules/{id}
```

**响应：**

```json
{
  "success": true,
  "code": 200,
  "data": null
}
```

---

## 六、状态码汇总

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数校验失败 |
| 404 | 股票/公司/任务不存在 |
| 409 | 资源冲突（如重复的唯一键）|
| 500 | 服务端内部错误 |
