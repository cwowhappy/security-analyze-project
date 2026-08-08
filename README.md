# Security Analyze Project

安全分析项目，包含前端服务、后端服务和数据收集服务三个模块，统一使用 PostgreSQL 持久化。

## 技术栈

| 模块 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue 3 + TypeScript + Vite | Vue 3.5.x, TS 5.7.x, Vite 6.x |
| 后端 | Java + Spring Boot + Gradle | Java 25, Spring Boot 4.1.x, Gradle 9.7.0+ |
| 数据收集 | Python + APScheduler + uv | Python 3.12+, APScheduler 3.11.x, uv 0.8.x |
| 数据库 | PostgreSQL | 17.x |

## 项目结构

```
security-analyze-project/
├── frontend/          # Vue 3 前端服务
├── backend/           # Spring Boot 后端服务
├── collector/         # Python 数据收集服务
├── docker-compose.yml # 本地全栈编排
└── README.md
```

## 快速开始

### 1. 本地开发环境要求

- Node.js 20/22
- Java 25
- Python 3.12+
- uv (Python package manager)
- Docker & Docker Compose

### 2. 使用 Docker Compose 启动

```bash
docker compose up -d
```

访问：
- 前端：http://localhost
- 后端 API：http://localhost:8080/api/v1
- Swagger UI：http://localhost:8080/swagger-ui/index.html

### 3. 各模块独立启动

#### 前端

```bash
cd frontend
npm install
npm run dev
```

#### 后端

```bash
cd backend
./gradlew bootRun
```

#### 数据收集服务

```bash
cd collector
uv sync
uv run src/main.py
```

## 默认账号

- 用户名：`admin`
- 密码：`admin`

> 生产环境请务必修改 `JWT_SECRET` 和默认管理员密码。

## API 说明

- `POST /api/v1/auth/login` - 登录
- `GET /api/v1/events` - 安全事件列表
- `POST /api/v1/events` - 创建安全事件

## 数据库迁移

后端使用 Flyway 管理数据库版本，迁移脚本位于 `backend/src/main/resources/db/migration/`。

## 数据采集

数据收集服务基于 APScheduler 调度，默认每 5 分钟执行一次示例 CVE 采集任务。可在 `collector/src/scheduler.py` 中添加更多任务。
