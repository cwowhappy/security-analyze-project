# 前端模块

基于 Vue 3 + TypeScript + Vite 构建。

## 技术栈

- Vue 3.5 (Composition API + `<script setup>`)
- TypeScript ~6.0
- Vite 8
- Pinia (状态管理)
- Vue Router 4
- Vitest (单元测试)
- ESLint 9 + Prettier

## 项目结构

```
src/
├── api/                  # API 接口层
│   └── modules/          # 按业务模块拆分
├── assets/               # 静态资源
│   ├── images/
│   └── styles/
├── components/           # 组件
│   ├── base/             # 基础组件 (BaseButton, BaseInput 等)
│   ├── business/         # 业务公共组件
│   └── __tests__/        # 组件测试
├── composables/          # 组合式函数
│   └── __tests__/        # Composable 测试
├── layouts/              # 页面布局
├── router/               # 路由配置
│   └── modules/          # 路由模块
├── stores/               # Pinia 状态管理
│   └── modules/          # Store 模块
├── types/                # 全局类型定义
├── utils/                # 工具函数
├── views/                # 页面组件
│   └── stock/            # 股票相关页面
├── App.vue
└── main.ts
```

## 开发规范

### 1. 代码规范

**组件命名**：
- 基础组件：`BaseButton`、`BaseInput`（统一前缀 Base）
- 业务组件：`StockList`、`StockDetail`
- 页面组件：`StockListView`、`StockDetailView`（以 View 结尾）
- 单例组件：`TheHeader`、`TheFooter`（以 The 开头）
- 所有组件名必须是多词，避免与 HTML 元素冲突

**文件命名**：
- 组件文件：PascalCase（如 `BaseButton.vue`）
- 其他文件：camelCase（如 `useRequest.ts`）

**TypeScript**：
- `strict: true` 必须开启
- Props/Emits 使用接口定义 + 泛型
- 避免使用 `any`

**依赖注入**：
- 优先使用 Composition API 和 composables
- Store 使用 Pinia Composition API 风格

### 2. ESLint + Prettier

已配置 ESLint 9 flat config + Prettier，解决冲突：

```bash
# 代码检查
npm run lint

# 自动修复
npm run lint:fix

# 代码格式化
npm run format
```

### 3. 测试规范

- 组件测试：使用 `@vue/test-utils` + Vitest
- Composable 测试：纯函数测试，不挂载组件
- 测试命名：`should{预期}When{条件}` 或 `{行为}_{条件}_{预期}`

```bash
# 运行测试
npm run test

# UI 模式
npm run test:ui
```

### 4. API 调用规范

- API 函数统一放在 `api/modules/` 下，按业务拆分
- 使用 `utils/request.ts` 封装，统一处理响应格式
- 返回类型使用 `ApiResponse<T>`

## 常用命令

```bash
npm install        # 安装依赖
npm run dev        # 启动开发服务器
npm run build      # 构建生产包
npm run preview    # 预览生产包
npm run test       # 运行单元测试
npm run lint       # 代码检查
npm run lint:fix   # 自动修复代码
npm run format     # 格式化代码
```
