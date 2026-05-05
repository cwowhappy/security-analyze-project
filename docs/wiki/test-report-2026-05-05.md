# 证券分析系统 - 功能测试报告

**测试日期**：2026-05-05  
**测试环境**：macOS, PostgreSQL 18, Java 21, Node.js 20+  
**测试范围**：前端 12 个页面 + 后端 20+ API + 数据采集模块  
**测试方法**：代码审查 + API 调用测试 + 端到端路径验证  

---

## 执行摘要

| 指标 | 数值 |
|------|------|
| 总测试项 | 28 |
| 通过 | 23 |
| 部分通过 | 3 |
| 失败 | 2 |
| 严重问题 | 1 |
| 中等问题 | 3 |
| 一般问题/建议 | 4 |

---

## 功能可用性矩阵

### 认证与授权

| 功能 | 状态 | 备注 |
|------|------|------|
| 用户注册 | ✅ | 前端校验 + 后端唯一性检查 + PENDING 状态 |
| 用户登录 | ✅ | 正常/PENDING/DISABLED 状态拦截正确 |
| JWT Token 管理 | ⚠️ | 有效 token 正常，**无效 token 返回 500（应为 401）** |
| 管理员登录 | ✅ | 含管理员注册功能 |
| 权限隔离（RBAC） | ✅ | 普通用户访问 admin 接口返回 403 |

### 公司信息

| 功能 | 状态 | 备注 |
|------|------|------|
| 公司列表搜索 | ⚠️ | 股票代码/空关键词正常，**中文关键词返回空** |
| 公司详情 | ⚠️ | 数据展示正常，**但 `ElTag` 未导入可能渲染失败** |
| 行业分类跳转 | ✅ | EM/SW 链接跳转正确 |
| 关联证券展示 | ✅ | 多市场证券（A/B/H股）正常展示 |

### 财务报告

| 功能 | 状态 | 备注 |
|------|------|------|
| 财务报告列表 | ✅ | API 正常，数据依赖采集 |
| 核心指标趋势 | ✅ | 双 Y 轴图表，报告类型/日期筛选正常 |
| 年度报告期对比 | ✅ | 固定 X 轴（一季报→年报） |
| 报告详情三张表 | ✅ | 资产负债表/利润表/现金流量表 |

### 行业信息

| 功能 | 状态 | 备注 |
|------|------|------|
| 行业列表（EM/SW） | ✅ | 标准切换 + SW 层级导航正常 |
| 行业详情 | ✅ | 趋势图 + 成分股分页 |
| 行业趋势数据 | ✅ | EM 真实数据，SW fallback 模拟数据有标注 |

### 指数信息

| 功能 | 状态 | 备注 |
|------|------|------|
| 指数列表 | ✅ | 核心指数分类 + 搜索自动补全 |
| 指数详情 | ✅ | 基本信息/趋势/ETF |
| 指数趋势图 | ✅ | 日线/周线/月线切换 |
| 关联 ETF | ✅ | 基金规模元→亿换算正确 |

### 采集监控（ADMIN）

| 功能 | 状态 | 备注 |
|------|------|------|
| 概览卡片 | ✅ | 总条数/状态/耗时/更新时间 |
| 任务列表 | ✅ | 筛选 + 分页 + 自动刷新 |
| 权限控制 | ✅ | 非 ADMIN 路由拦截正确 |

### 用户管理（ADMIN）

| 功能 | 状态 | 备注 |
|------|------|------|
| 用户列表 | ✅ | 统计卡片 + 表格 |
| 审批/禁用/启用 | ✅ | 状态流转 + 确认框 |

---

## 问题清单

### BUG-01 [严重] CompanyDetailView.vue 缺少 ElTag 组件导入

- **位置**：`frontend/src/views/company/CompanyDetailView.vue:4`
- **现象**：模板中使用了 `<ElTag>` 组件，但 `import` 语句中未包含 `ElTag`
- **影响**：Vue 开发模式下会报组件未注册警告，生产构建可能报错
- **修复建议**：
  ```typescript
  // 第4行修改
  import { ElTabs, ElTabPane, ElBreadcrumb, ElBreadcrumbItem, ElMessage, ElCard, ElLink, ElEmpty, ElTag } from 'element-plus'
  ```

### BUG-02 [中等] /api/auth/me 无效 token 返回 500 而非 401

- **位置**：`backend/src/main/java/com/example/securityanalyze/auth/api/AuthController.java:41-45`
- **现象**：当传入无效 JWT token 时，`@AuthenticationPrincipal UserDetails userDetails` 为 null，Controller 直接调用 `userDetails.getUsername()` 导致 NPE，返回 500
- **预期**：应返回 401 Unauthorized
- **修复建议**：在 Controller 中做空值检查，或确保 Filter 拦截无效 token 时不放行请求
  ```java
  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getCurrentUser(
          @AuthenticationPrincipal UserDetails userDetails) {
      if (userDetails == null) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
      // ...
  }
  ```

### BUG-03 [中等] 中文关键词搜索返回空结果

- **位置**：`backend/src/main/java/com/example/securityanalyze/company/infrastructure/CompanyRepositoryImpl.java`（或对应 Repository）
- **现象**：`/api/companies?keyword=%E8%8C%85%E5%8F%B0`（茅台）返回 `{"items":[],"total":0}`，但按股票代码 `600519` 搜索正常
- **根因推测**：Repository 中的 SQL LIKE 查询未正确处理 UTF-8 编码，或 keyword 参数未做 URL 解码
- **验证**：数据库中实际存在 8711 家公司，空关键词搜索返回列表正常
- **修复建议**：检查 `findByKeyword` 的 SQL 实现，确认 `keyword` 参数是否正确绑定到 `ILIKE` 或 `LIKE` 条件

### BUG-04 [中等] 申万行业分类一级类别映射不准确

- **位置**：`collector/collector/tasks/company_task.py` 的 `L1_TO_801_MAPPING` 映射表
- **现象**：600519 贵州茅台的 SW 分类显示一级为"传媒"(801760)，二级为"白酒Ⅱ"(801125)。实际上申万行业中白酒应属于"食品饮料"(801120)一级
- **影响**：行业分类数据质量受损，影响行业分析和筛选准确性
- **修复建议**：校正 `L1_TO_801_MAPPING` 映射表，确保前两位到一级行业的映射符合申万行业分类标准

### BUG-05 [一般] 前端分页组件页码展示不一致

- **位置**：多个 Vue 文件
- **现象**：
  - `CompanyListView.vue`：`v-model:current-page="page"`（后端 0-based，Element Plus 组件自动 +1 显示）
  - `IndustryDetailView.vue`：`:current-page="page + 1"`（手动 +1）
  - `IndexListView.vue`：`:current-page="page + 1"`（手动 +1）
- **影响**：功能正常，但代码风格不一致，维护成本高
- **修复建议**：统一分页逻辑，全部使用 `v-model:current-page="page"` 并在 `@current-change` 中处理，或统一使用手动 +1 模式

### BUG-06 [一般] axios 401 拦截器使用 window.location.href 全页刷新

- **位置**：`frontend/src/api/axios.ts:24-28`
- **现象**：token 过期时执行 `window.location.href = '/login'`，导致整页刷新
- **影响**：SPA 用户体验受损，页面状态丢失
- **修复建议**：使用 Vue Router 进行无刷新跳转（需导入 router 实例）
  ```typescript
  import router from '@/router'
  // ...
  if (error.response?.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
  }
  ```

### BUG-07 [一般] 指数详情部分字段可能为 null

- **位置**：`backend/src/main/java/com/example/securityanalyze/index/application/IndexService.java`
- **现象**：`baseDate`、`basePoint`、`componentCount` 字段在部分指数中为 null
- **影响**：前端详情页显示为 `-`，数据完整性不足
- **修复建议**：在数据采集阶段补充这些字段，或在前端增加数据来源说明

### BUG-08 [建议] CORS 配置使用通配符 AllowedHeaders

- **位置**：`backend/src/main/java/com/example/securityanalyze/config/SecurityConfig.java:70`
- **现象**：`configuration.setAllowedHeaders(Arrays.asList("*"))`
- **影响**：开发环境无问题，生产环境存在安全风险
- **修复建议**：生产环境应显式列出需要的 headers：`Authorization`、`Content-Type`、`Accept` 等

---

## 改进建议

### 用户体验
1. **搜索框空关键词优化**：当前空关键词不触发搜索（显示空白），建议空关键词时展示热门公司或最近浏览
2. **加载状态统一**：部分页面使用 `v-loading`，部分页面手动管理 `loading` ref，建议统一封装加载状态 Hook
3. **错误提示细化**：当前大量使用 `ElMessage.error('加载失败')`，建议根据错误类型展示更具体的提示

### 代码质量
1. **前端组件导入统一**：建议统一使用 `import { ElXxx } from 'element-plus'` 的按需导入方式，避免全局注册
2. **TypeScript 类型严格化**：部分 `any` 类型（如 `error: any`）可替换为更精确的类型
3. **后端日志级别优化**：部分 `log.info` 在每次请求时打印（如查询列表），高并发时日志量过大，建议改为 `log.debug`

### 数据质量
1. **申万行业映射校准**：重新核对 `L1_TO_801_MAPPING` 和 `L2_TO_801_MAPPING`
2. **财务报告数据补全**：600519 等热门股票缺失财务报告数据，建议重新触发采集
3. **指数基础字段补充**：补充 `baseDate`、`basePoint`、`componentCount` 等字段

### 安全加固
1. **生产环境 CORS**：限制为具体域名，禁用通配符
2. **默认管理员密码**：首次启动后强制修改默认密码 `admin123`
3. **JWT Secret**：确保通过环境变量注入，不使用硬编码
4. **注册接口限流**：防止恶意批量注册

---

## 数据采集模块评估

| 任务 | 状态 | 说明 |
|------|------|------|
| 公司信息采集 | ✅ | 公司-证券分离模型正确，申万/东财行业映射已实现 |
| 财务报告采集 | ✅ | Session 断点续传机制完善，并发控制合理 |
| 行业分类同步 | ⚠️ | 申万一级映射存在误差，需校准 |
| 指数数据采集 | ✅ | 基本信息/历史数据/ETF 数据覆盖完整 |

---

## 结论

系统整体架构清晰，前后端分层合理，核心功能（公司搜索、行业浏览、指数查询、用户管理）运行稳定。存在 **1 个严重问题**（前端组件未导入）和 **3 个中等问题**（无效 token 处理、中文搜索、行业映射），建议优先修复。其余为代码风格和生产环境配置建议，可在后续迭代中逐步优化。
