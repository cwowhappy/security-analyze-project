# 后端模块设计

> 本文档描述 Java 后端新增的领域模块、分层结构与 REST API 规划。

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
└── collection/               # 新建模块
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
| `CompanyRepository.java` | 仓库端口：findById / findByUscCode / findAll / save / deleteById |

### 2.2 应用层（Application）

| 类 | 职责 |
|----|------|
| `CompanyDTO.java` | 数据传输对象 |
| `CompanyAppService.java` | 应用服务接口：查询列表、按统一社会信用代码查询、创建公司 |
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
| `CompanyController.java` | REST 控制器：`/api/companies` |
| `CreateCompanyRequest.java` | 创建请求 DTO（含字段校验注解）|

---

## 三、Collection 模块

### 3.1 领域层（Domain）

| 类 | 职责 |
|----|------|
| `CollectionTask.java` | 领域实体：采集任务状态与统计 |
| `CollectionTaskId.java` | 值对象 |
| `CollectionTaskRepository.java` | 仓库端口 |
| `CollectionTaskSchedule.java` | 领域实体：定时规则 |
| `CollectionTaskScheduleRepository.java` | 仓库端口 |

### 3.2 应用层（Application）

| 类 | 职责 |
|----|------|
| `CollectionTaskDTO.java` | 任务 DTO |
| `CollectionTaskAppService.java` | 任务查询、创建即时任务 |
| `CollectionTaskScheduleDTO.java` | 规则 DTO |
| `CollectionTaskScheduleAppService.java` | 规则增删改查、启停 |

### 3.3 基础设施层（Infrastructure）

| 类 | 职责 |
|----|------|
| `CollectionTaskEntity.java` | 映射 `tb_collection_task` |
| `CollectionTaskScheduleEntity.java` | 映射 `tb_collection_task_schedule` |
| `JdbcCollectionTaskRepository.java` | JDBC 实现 |
| `JdbcCollectionTaskScheduleRepository.java` | JDBC 实现 |

### 3.4 接口层（Interfaces）

| 类 | 职责 |
|----|------|
| `CollectionTaskController.java` | `/api/collection/tasks` |
| `CollectionTaskScheduleController.java` | `/api/collection/schedules` |

---

## 四、Stock 模块扩展

现有 `stock` 模块需做以下调整：

1. **实体字段调整**：
   - 移除 `companyId`、`currentPrice`、`changePercent`
   - 新增 `stockCode`（替换 `symbol`）、`tsCode`、`fullName`、`listDate`、`industry`、`area`、`totalShares`、`floatShares`

2. **关联查询**：
   - `StockController.getByStockCode()` 返回股票详情时，通过 `tb_relation_stock_company` 关联查询对应公司信息

3. **表名调整**：
   - 持久化实体从 `stock` 表迁移到 `tb_stock_basic`

---

## 五、REST API 清单

### 5.1 股票 API

| 方法 | 路径 | 功能 | 响应 |
|------|------|------|------|
| GET | `/api/stocks` | 股票列表（分页） | `PageResult<StockDTO>` |
| GET | `/api/stocks/{stockCode}` | 股票详情 | `StockDTO` + 关联 `CompanyDTO` |

### 5.2 公司 API

| 方法 | 路径 | 功能 | 响应 |
|------|------|------|------|
| GET | `/api/companies` | 公司列表（分页，支持 industry/province 筛选） | `PageResult<CompanyDTO>` |
| GET | `/api/companies/{uscCode}` | 公司详情 | `CompanyDTO` + 关联股票列表 |

### 5.3 采集任务 API

| 方法 | 路径 | 功能 | 响应 |
|------|------|------|------|
| GET | `/api/collection/tasks` | 任务历史列表（分页） | `PageResult<CollectionTaskDTO>` |
| POST | `/api/collection/tasks` | 创建即时采集任务 | `CollectionTaskDTO` |
| GET | `/api/collection/tasks/{id}` | 任务详情/进度 | `CollectionTaskDTO` |

### 5.4 定时规则 API

| 方法 | 路径 | 功能 | 响应 |
|------|------|------|------|
| GET | `/api/collection/schedules` | 定时规则列表 | `List<CollectionTaskScheduleDTO>` |
| POST | `/api/collection/schedules` | 创建定时规则 | `CollectionTaskScheduleDTO` |
| PUT | `/api/collection/schedules/{id}` | 更新/启停规则 | `CollectionTaskScheduleDTO` |
| DELETE | `/api/collection/schedules/{id}` | 删除规则 | 空 |

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
