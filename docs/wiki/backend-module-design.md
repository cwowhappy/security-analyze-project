# 后端模块设计 v2

> 本文档描述 Java 后端新增的领域模块、分层结构与 REST API 规划。  
> 版本：v2.0 | 变更：去除定时规则模块，股票表直接外键关联公司，API 统一 `/api/v1` 前缀。

---

## 一、模块总览

后端在现有 `stock` 模块基础上，新增 `company` 和 `collection` 两个领域模块，均采用 DDD 分层架构。

```
backend/src/main/java/org/cwowhappy/securityanalyze/
├── stock/                    # 扩展现有模块
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
├── company/                  # 新建模块
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
└── collection/               # 新建模块（不含 schedule）
    ├── domain/
    ├── application/
    ├── infrastructure/
    └── interfaces/
```

---

## 二、Company 模块

### 2.1 领域层（Domain）

| 类 | 职责 |
|----|------|
| `Company.java` | 领域实体：封装公司属性与业务规则 |
| `CompanyId.java` | 值对象：基于 UUID 的 ID 生成 |
| `CompanyRepository.java` | 仓库端口：findById / findByUscCode / findAll / save |

### 2.2 应用层（Application）

| 类 | 职责 |
|----|------|
| `CompanyDTO.java` | 数据传输对象 |
| `CompanyAppService.java` | 应用服务：查询列表、按统一社会信用代码查询 |
| `CompanyAppServiceImpl.java` | 实现 |

### 2.3 基础设施层（Infrastructure）

| 类 | 职责 |
|----|------|
| `CompanyEntity.java` | JDBC 持久化 POJO，映射 `tb_company_basic` |
| `CompanyRowMapper.java` | `RowMapper<CompanyEntity>` |
| `JdbcCompanyRepository.java` | `CompanyRepository` 的 JDBC 实现，Upsert 语义 |

### 2.4 接口层（Interfaces）

| 类 | 职责 |
|----|------|
| `CompanyController.java` | REST 控制器：`/api/v1/companies` |

---

## 三、Collection 模块

### 3.1 领域层（Domain）

| 类 | 职责 |
|----|------|
| `CollectionTask.java` | 领域实体：采集任务状态与统计 |
| `CollectionTaskId.java` | 值对象 |
| `CollectionTaskRepository.java` | 仓库端口 |

> **注意**：v2.0 去除 `CollectionTaskSchedule` 实体与相关仓库。

### 3.2 应用层（Application）

| 类 | 职责 |
|----|------|
| `CollectionTaskDTO.java` | 任务 DTO |
| `CollectionTaskAppService.java` | 任务查询、创建即时任务 |

### 3.3 基础设施层（Infrastructure）

| 类 | 职责 |
|----|------|
| `CollectionTaskEntity.java` | 映射 `tb_collection_task` |
| `JdbcCollectionTaskRepository.java` | JDBC 实现 |

### 3.4 接口层（Interfaces）

| 类 | 职责 |
|----|------|
| `CollectionTaskController.java` | `/api/v1/collection/tasks` |

---

## 四、Stock 模块扩展

现有 `stock` 模块需做以下调整：

1. **实体字段调整**：
   - 移除 `currentPrice`、`changePercent`
   - 新增 `stockCode`（替换 `symbol`）、`tsCode`、`fullName`、`listDate`、`industry`、`area`、`totalShares`、`floatShares`
   - **新增 `companyId`**：直接外键关联 `tb_company_basic.id`

2. **关联查询**：
   - `StockController.getByStockCode()` 返回股票详情时，通过 `tb_stock_basic.company_id` JOIN `tb_company_basic` 获取关联公司信息

3. **表名调整**：
   - 持久化实体从 `stock` 表迁移到 `tb_stock_basic`

---

## 五、REST API 清单

### 5.1 股票 API

| 方法 | 路径 | 功能 | 响应 |
|------|------|------|------|
| GET | `/api/v1/stocks` | 股票列表（分页、筛选、搜索） | `PageResult<StockDTO>` |
| GET | `/api/v1/stocks/{stockCode}` | 股票详情（含关联公司信息） | `StockDTO` + `CompanyBriefDTO` |

### 5.2 公司 API

| 方法 | 路径 | 功能 | 响应 |
|------|------|------|------|
| GET | `/api/v1/companies` | 公司列表（分页、筛选、搜索） | `PageResult<CompanyDTO>` |
| GET | `/api/v1/companies/{uscCode}` | 公司详情（含关联股票列表） | `CompanyDTO` + `List<StockBriefDTO>` |

### 5.3 采集任务 API

| 方法 | 路径 | 功能 | 响应 |
|------|------|------|------|
| GET | `/api/v1/collection/tasks` | 任务历史列表（分页、筛选） | `PageResult<CollectionTaskDTO>` |
| POST | `/api/v1/collection/tasks` | 创建即时采集任务 | `CollectionTaskDTO` |
| GET | `/api/v1/collection/tasks/{id}` | 任务详情/进度 | `CollectionTaskDTO` |

### 5.4 首页统计 API（P1）

| 方法 | 路径 | 功能 | 响应 |
|------|------|------|------|
| GET | `/api/v1/dashboard/stats` | 首页核心统计数据 | `DashboardStatsDTO` |

> **注意**：v2.0 去除 `/api/v1/collection/schedules` 全部接口（列表、创建、更新、删除）。定时规则由采集器内部管理。

---

## 六、统一响应格式

沿用现有 `ApiResponse<T>` 包装器：

```json
{
  "success": true,
  "code": 200,
  "message": "OK",
  "data": { ... },
  "timestamp": 1715340000000
}
```

---

## 七、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.0 | 2026-05-11 | 去除 schedule 模块，股票直接外键关联公司，API 统一 `/api/v1` |
| v1.0 | 2026-05-10 | 初始版本（已废弃） |
