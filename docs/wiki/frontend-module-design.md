# 前端模块设计 v2

> 本文档描述 Vue 3 前端新增页面、组件与路由规划。  
> 版本：v2.0 | 变更：去除定时规则管理页面，API 统一 `/api/v1` 前缀，严格对齐 `prototype-stock-company.html`。

---

## 一、页面清单

| 页面 | 路由 | 功能描述 |
|------|------|---------|
| 首页概览 | `/` | 统计卡片 + 功能入口快捷跳转 |
| 股票列表 | `/stocks` | 表格展示股票列表，支持市场/行业/地域筛选与关键词搜索 |
| 股票详情 | `/stocks/:stockCode` | 股票基础信息 + 上市信息 + 股本信息 + 关联公司卡片 |
| 公司列表 | `/companies` | 表格展示公司列表，支持行业/省份/企业性质筛选与关键词搜索 |
| 公司详情 | `/companies/:uscCode` | 工商信息 + 管理层 + 业务与治理 + 主营业务/经营范围/简介 + 关联股票列表 |
| 采集任务 | `/collection/tasks` | 统计卡片 + 任务列表 + 手动触发 + 重新执行 + 状态监控 |

> **注意**：v2.0 去除 `/collection/schedules` 定时规则管理页面。定时规则由采集器内部管理，前端不感知。

---

## 二、路由配置

```typescript
// router/index.ts 新增路由
const routes = [
  { path: '/', component: () => import('@/views/HomeView.vue') },
  { path: '/stocks', component: () => import('@/views/stock/StockListView.vue') },
  { path: '/stocks/:stockCode', component: () => import('@/views/stock/StockDetailView.vue') },
  { path: '/companies', component: () => import('@/views/company/CompanyListView.vue') },
  { path: '/companies/:uscCode', component: () => import('@/views/company/CompanyDetailView.vue') },
  { path: '/collection/tasks', component: () => import('@/views/collection/CollectionTaskView.vue') },
];
```

---

## 三、页面设计

### 3.1 HomeView（首页概览）

对齐原型 `p-home`：

- **欢迎卡片**：用户问候 + 市场状态提示
- **统计行**（4 列网格）：
  - A股上市公司总数
  - 数据覆盖股票数
  - 下次定时采集时间
  - 最近采集成功率
- **功能网格**（3 列或响应式）：
  - 股票列表（已上线）
  - 公司列表（已上线）
  - 采集任务（已上线）
  - 数据分析（即将上线，占位）
  - 投资管理（即将上线，占位）
  - 风险管理（即将上线，占位）

### 3.2 StockListView（股票列表）

对齐原型 `p-stock-list`：

- **筛选栏**：
  - 关键词搜索（input，placeholder：搜索股票代码或名称...）
  - 市场筛选（select：全部/主板/创业板/科创板/北交所）
  - 行业筛选（select：动态加载）
  - 地域筛选（select：动态加载）
- **数据表格**：
  - 列：股票代码、股票简称、交易所（徽章）、市场类型、行业、地域、总股本(亿股)、流通股本(亿股)、上市日期、操作（详情按钮）
  - 股票代码高亮样式（`color: var(--primary)`，等宽字体）
  - 交易所徽章：`.b-sh` / `.b-sz` / `.b-bj`

### 3.3 StockDetailView（股票详情）

对齐原型 `p-stock-detail`：

- **返回按钮**：← 返回股票列表
- **头部卡片**：
  - 股票代码（高亮等宽）+ 股票简称 + 交易所徽章 + 市场类型标签
  - 股票全称（副标题）
  - "查看关联公司"按钮（跳转到对应公司详情）
- **基础信息区块**（3 列网格）：
  - 股票代码 / Tushare代码 / 股票简称 / 股票全称 / 交易所代码 / 市场类型
- **上市信息区块**（3 列）：
  - 上市日期 / 所属行业 / 所属地域
- **股本信息区块**（3 列）：
  - 总股本（股）/ 流通股本（股）/ 总股本（亿股）/ 流通股本（亿股）
- **关联公司区块**：
  - 公司名称 + 统一信用代码 + 查看详情按钮

### 3.4 CompanyListView（公司列表）

对齐原型 `p-company-list`：

- **筛选栏**：
  - 关键词搜索（input，placeholder：搜索公司名称或信用代码...）
  - 行业筛选（select）
  - 省份筛选（select）
  - 企业性质筛选（select：全部/国企/民营/外资/其他）
- **数据表格**：
  - 列：公司全称、简称、行业、省份、城市、企业性质（徽章）、法人代表、员工人数（千分位）、操作（详情按钮）
  - 企业性质徽章：`.bs-gq`（国企）/ `.bs-my`（民营）/ `.bs-wz`（外资）/ `.bs-qt`（其他）

### 3.5 CompanyDetailView（公司详情）

对齐原型 `p-company-detail`：

- **返回按钮**：← 返回公司列表
- **头部卡片**：
  - 公司全称 + 企业性质徽章
  - 公司简称 + 英文名称（副标题）
  - "查看关联股票"按钮
- **工商注册信息区块**（3 列）：
  - 统一信用代码 / 公司全称 / 公司简称 / 英文名称 / 曾用简称 / 注册资本(万元)
- **成立与地址信息区块**（3 列）：
  - 成立日期 / 所在省份 / 所在城市 / 注册地址 / 办公地址 / 官方网站
- **管理层信息区块**（3 列）：
  - 法人代表 / 董事长 / 总经理 / 董事会秘书 / 员工人数
- **业务与治理信息区块**（3 列）：
  - 所属行业 / 实控人名称 / 实控人性质
- **主营业务区块**：文本块展示 `mainBusiness`
- **经营范围区块**：文本块展示 `businessScope`
- **公司简介区块**：文本块展示 `introduction`
- **关联股票区块**：
  - 股票代码 + 名称 + 交易所徽章 + 查看详情按钮

### 3.6 CollectionTaskView（采集任务）

对齐原型 `p-tasks`：

- **页面标题**：采集任务
- **筛选栏**：
  - 关键词搜索（任务名称/类型）
  - "触发采集任务"按钮（弹出选择任务类型后创建）
- **统计卡片行**（4 列）：
  - 总任务数
  - 执行成功（绿色）
  - 执行中（蓝色）
  - 执行失败（红色）
- **任务卡片列表**：
  - 任务名称（加粗）
  - 元信息行：任务类型 / 执行时间 / 执行时长 / 处理条数 / 成功数 / 失败数
  - 进度条：`success_count / total_count * 100%`
  - 状态徽章 + 重新执行按钮
  - 状态样式：成功(绿 `.ss`) / 执行中(蓝 `.sr` + 呼吸灯) / 失败(红 `.sf`)

---

## 四、TypeScript 类型定义

```typescript
// types/stock.ts（扩展）
export interface Stock {
  id: string;
  stockCode: string;
  tsCode?: string;
  name: string;
  fullName?: string;
  market?: string;
  exchange?: string;
  listDate?: string;
  industry?: string;
  area?: string;
  totalShares?: number;
  floatShares?: number;
  companyId?: string;
  updatedAt?: string;
}

export interface StockDetail extends Stock {
  company?: CompanyBrief;
}

export interface StockBrief {
  stockCode: string;
  name: string;
  market?: string;
  exchange?: string;
  listDate?: string;
}

// types/company.ts（新增）
export interface Company {
  id: string;
  unifiedSocialCreditCode?: string;
  name: string;
  shortName?: string;
  englishName?: string;
  formerName?: string;
  legalRepresentative?: string;
  chairman?: string;
  manager?: string;
  secretary?: string;
  regCapital?: number;
  setupDate?: string;
  province?: string;
  city?: string;
  regAddress?: string;
  officeAddress?: string;
  website?: string;
  industry?: string;
  mainBusiness?: string;
  businessScope?: string;
  introduction?: string;
  employees?: number;
  controllerName?: string;
  controllerType?: string;
}

export interface CompanyDetail extends Company {
  stocks?: StockBrief[];
}

export interface CompanyBrief {
  id: string;
  unifiedSocialCreditCode?: string;
  name: string;
  shortName?: string;
  legalRepresentative?: string;
  regCapital?: number;
  setupDate?: string;
  mainBusiness?: string;
}

// types/collection.ts（新增）
export type TaskType = 'stock_full' | 'company_full' | 'stock_single' | 'company_single';
export type TaskStatus = 'pending' | 'running' | 'success' | 'failed';

export interface CollectionTask {
  id: string;
  taskType: TaskType;
  taskParams?: Record<string, unknown>;
  status: TaskStatus;
  dataSource?: string;
  totalCount: number;
  successCount: number;
  failCount: number;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
}

// types/dashboard.ts（新增，P1）
export interface DashboardStats {
  totalCompanies: number;
  totalStocks: number;
  nextScheduledTime?: string;
  lastSuccessRate: number;
}
```

---

## 五、API 调用封装

```typescript
// api/modules/stock.ts（扩展）
export const stockApi = {
  list: (params?: PageQuery & { market?: string; industry?: string; area?: string; keyword?: string }) =>
    request.get<PageResult<Stock>>('/api/v1/stocks', { params }),
  getByCode: (stockCode: string) =>
    request.get<ApiResponse<StockDetail>>(`/api/v1/stocks/${stockCode}`),
};

// api/modules/company.ts（新增）
export const companyApi = {
  list: (params?: PageQuery & { industry?: string; province?: string; controllerType?: string; keyword?: string }) =>
    request.get<PageResult<Company>>('/api/v1/companies', { params }),
  getByUscCode: (uscCode: string) =>
    request.get<ApiResponse<CompanyDetail>>(`/api/v1/companies/${uscCode}`),
};

// api/modules/collection.ts（新增）
export const collectionApi = {
  listTasks: (params?: PageQuery & { status?: TaskStatus; taskType?: TaskType }) =>
    request.get<PageResult<CollectionTask>>('/api/v1/collection/tasks', { params }),
  createTask: (data: { taskType: TaskType; taskParams?: Record<string, unknown>; dataSource?: string }) =>
    request.post<ApiResponse<CollectionTask>>('/api/v1/collection/tasks', data),
  getTask: (id: string) =>
    request.get<ApiResponse<CollectionTask>>(`/api/v1/collection/tasks/${id}`),
};

// api/modules/dashboard.ts（新增，P1）
export const dashboardApi = {
  getStats: () => request.get<ApiResponse<DashboardStats>>('/api/v1/dashboard/stats'),
};
```

---

## 六、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.0 | 2026-05-11 | 去除定时规则页面与类型，增加首页概览页面，API 统一 `/api/v1`，严格对齐原型 |
| v1.0 | 2026-05-10 | 初始版本（已废弃） |
