# 前端模块设计

> 本文档描述 Vue 3 前端新增页面、组件与路由规划。

---

## 一、页面清单

| 页面 | 路由 | 功能描述 |
|------|------|---------|
| 股票详情 | `/stocks/:stockCode` | 展示股票基础信息卡片 + 关联公司信息卡片 |
| 公司列表 | `/companies` | 表格展示公司列表，支持行业/地域筛选 |
| 公司详情 | `/companies/:uscCode` | 展示公司工商信息 + 关联股票列表 |
| 采集任务管理 | `/collection/tasks` | 任务列表、手工触发、执行状态监控 |
| 定时规则管理 | `/collection/schedules` | 规则增删改查、启停控制 |

---

## 二、路由配置

```typescript
// router/index.ts 新增路由
const routes = [
  // 现有路由保留
  { path: '/', component: StockListView },
  { path: '/stocks', component: StockListView },
  // 新增路由
  { path: '/stocks/:stockCode', component: () => import('@/views/stock/StockDetailView.vue') },
  { path: '/companies', component: () => import('@/views/company/CompanyListView.vue') },
  { path: '/companies/:uscCode', component: () => import('@/views/company/CompanyDetailView.vue') },
  { path: '/collection/tasks', component: () => import('@/views/collection/CollectionTaskView.vue') },
  { path: '/collection/schedules', component: () => import('@/views/collection/CollectionScheduleView.vue') },
];
```

---

## 三、页面设计

### 3.1 StockDetailView（股票详情）

布局：两栏卡片

```
┌────────────────────────────────────────┐
│ 面包屑：股票列表 > 000001 平安银行         │
├────────────────────┬───────────────────┤
│  股票基础信息        │   关联公司信息      │
│  ─────────────     │   ─────────────   │
│  股票代码：000001   │   公司全称：...    │
│  股票简称：平安银行  │   法人代表：...    │
│  市场类型：主板     │   注册资本：...    │
│  上市日期：1991-04-03 │  成立日期：...   │
│  所属行业：银行     │   主营业务：...    │
│  总股本：194亿      │   [查看公司详情]   │
│  流通股本：194亿    │                   │
└────────────────────┴───────────────────┘
```

### 3.2 CompanyListView（公司列表）

布局：筛选栏 + 表格

- 筛选条件：行业（下拉）、省份（下拉）、关键词搜索（公司名称）
- 表格列：公司全称、所属行业、省份、成立日期、注册资本、操作（查看详情）
- 分页：沿用现有 `PageQuery` / `PageResult`

### 3.3 CompanyDetailView（公司详情）

布局：信息卡片 + 关联股票表格

- 公司工商信息卡片（两列布局）：法人代表、董事长、总经理、注册资本、成立日期、注册地址、办公地址、官网、主营业务、经营范围
- 关联股票表格：股票代码、股票简称、市场类型、上市日期

### 3.4 CollectionTaskView（采集任务管理）

布局：操作栏 + 任务列表

- 操作栏：
  - [股票全量采集] 按钮 → 调用 POST /api/collection/tasks
  - [公司全量采集] 按钮
  - 刷新按钮
- 任务列表列：任务类型、数据源、状态、成功/失败数、开始时间、完成时间、错误信息
- 状态标签：pending（灰色）、running（蓝色）、success（绿色）、failed（红色）

### 3.5 CollectionScheduleView（定时规则管理）

布局：规则列表 + 新增/编辑弹窗

- 列表列：规则名称、任务类型、数据源、Cron 表达式、启用状态、上次触发时间、操作
- 操作：编辑、启用/停用、删除
- 弹窗表单：名称、任务类型（下拉）、数据源（下拉）、Cron 表达式（输入框 + 常用模板）、参数（JSON 可选）

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
  updatedAt?: string;
}

// types/company.ts（新增）
export interface Company {
  id: string;
  unifiedSocialCreditCode?: string;
  name: string;
  shortName?: string;
  englishName?: string;
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
  scheduledAt?: string;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
}

export interface CollectionTaskSchedule {
  id: string;
  name: string;
  taskType: TaskType;
  taskParams?: Record<string, unknown>;
  dataSource?: string;
  cronExpression: string;
  isEnabled: boolean;
  lastTriggeredAt?: string;
}
```

---

## 五、API 调用封装

```typescript
// api/modules/stock.ts（扩展）
export const stockApi = {
  list: (params?: PageQuery) => request.get<PageResult<Stock>>('/api/stocks', { params }),
  getByCode: (stockCode: string) => request.get<ApiResponse<Stock & { company?: Company }>>(`/api/stocks/${stockCode}`),
};

// api/modules/company.ts（新增）
export const companyApi = {
  list: (params?: PageQuery & { industry?: string; province?: string }) =>
    request.get<PageResult<Company>>('/api/companies', { params }),
  getByUscCode: (uscCode: string) => request.get<ApiResponse<Company & { stocks?: Stock[] }>>(`/api/companies/${uscCode}`),
};

// api/modules/collection.ts（新增）
export const collectionApi = {
  listTasks: (params?: PageQuery) => request.get<PageResult<CollectionTask>>('/api/collection/tasks', { params }),
  createTask: (data: { taskType: TaskType; taskParams?: Record<string, unknown>; dataSource?: string }) =>
    request.post<CollectionTask>('/api/collection/tasks', data),
  getTask: (id: string) => request.get<CollectionTask>(`/api/collection/tasks/${id}`),
  listSchedules: () => request.get<CollectionTaskSchedule[]>('/api/collection/schedules'),
  createSchedule: (data: Omit<CollectionTaskSchedule, 'id'>) =>
    request.post<CollectionTaskSchedule>('/api/collection/schedules', data),
  updateSchedule: (id: string, data: Partial<CollectionTaskSchedule>) =>
    request.put<CollectionTaskSchedule>(`/api/collection/schedules/${id}`, data),
  deleteSchedule: (id: string) => request.delete(`/api/collection/schedules/${id}`),
};
```
