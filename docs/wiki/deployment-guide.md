# 证券分析系统部署文档

本文档描述证券分析系统（security-analyze）的完整部署流程，涵盖 PostgreSQL 数据库（独立部署）、后端服务、前端服务及数据采集服务的部署与配置。

> **注意**：PostgreSQL 采用独立部署方式，不通过 Docker 运行。

---

## 目录

1. [系统架构](#1-系统架构)
2. [环境要求](#2-环境要求)
3. [PostgreSQL 数据库部署](#3-postgresql-数据库部署)
4. [数据库初始化脚本](#4-数据库初始化脚本)
5. [后端服务部署](#5-后端服务部署)
6. [前端服务部署](#6-前端服务部署)
7. [数据采集服务部署](#7-数据采集服务部署)
8. [服务启动顺序](#8-服务启动顺序)
9. [生产环境部署建议](#9-生产环境部署建议)
10. [常见问题排查](#10-常见问题排查)

---

## 1. 系统架构

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   前端 (Vue 3)   │─────▶│  后端 (Spring   │─────▶│ PostgreSQL 16+  │
│   Port: 3000    │      │    Boot 3.5)    │      │   Port: 5432    │
└─────────────────┘      │   Port: 8080    │      │  (独立部署)      │
                         └─────────────────┘      └─────────────────┘
                                                          ▲
                                                          │
                              ┌─────────────────┐         │
                              │ 数据采集 (Python) │─────────┘
                              │  按需 / 常驻调度  │
                              └─────────────────┘
```

| 服务 | 技术栈 | 默认端口 | 说明 |
|------|--------|---------|------|
| 前端 | Vue 3 + TypeScript + Vite | 3000 | 开发服务器 / 静态文件 |
| 后端 | Java 21 + Spring Boot 3.5 | 8080 | REST API 服务 |
| 数据库 | PostgreSQL 16+ | 5432 | 独立安装部署 |
| 数据采集 | Python 3.11 + Poetry | — | 手动执行或常驻调度 |

---

## 2. 环境要求

### 2.1 必备软件

| 软件 | 版本 | 说明 |
|------|------|------|
| PostgreSQL | ≥ 16 | 独立安装，不通过 Docker |
| Java JDK | 21 | 后端编译运行 |
| Gradle | 9.4（已自带 Wrapper） | 后端构建 |
| Node.js | ≥ 18 | 前端构建 |
| npm | ≥ 9 | 前端依赖管理 |
| Python | ≥ 3.11 | 数据采集服务 |
| Poetry | ≥ 2.0（推荐）或 pip | 采集模块依赖管理 |

### 2.2 验证命令

```bash
# PostgreSQL
psql --version           # 应显示 PostgreSQL 16.x 或更高
pg_isready               # 检查服务是否运行

# Java
java -version            # 应显示 OpenJDK 21

# Node.js
node --version           # ≥ 18.x
npm --version

# Python
python3 --version        # ≥ 3.11
poetry --version         # ≥ 2.0（如使用 Poetry）
```

---

## 3. PostgreSQL 数据库部署

PostgreSQL 采用**独立部署**方式，需在宿主机直接安装并运行 PostgreSQL 服务。

### 3.1 安装 PostgreSQL

#### macOS（Homebrew）

```bash
# 安装 PostgreSQL
brew install postgresql@16

# 加入 PATH（如未自动加入）
echo 'export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# 启动服务
brew services start postgresql@16

# 或手动启动
pg_ctl -D /opt/homebrew/var/postgresql@16 start
```

#### Ubuntu / Debian

```bash
# 添加官方仓库
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -

# 安装
sudo apt update
sudo apt install postgresql-16 postgresql-client-16

# 启动服务
sudo systemctl enable postgresql
sudo systemctl start postgresql
```

#### CentOS / RHEL / Fedora

```bash
# 添加官方仓库
sudo dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-9-x86_64/pgdg-redhat-repo-latest.noarch.rpm

# 安装
sudo dnf install -y postgresql16-server postgresql16-contrib

# 初始化数据库
sudo /usr/pgsql-16/bin/postgresql-16-setup initdb

# 启动服务
sudo systemctl enable postgresql-16
sudo systemctl start postgresql-16
```

#### Windows

1. 下载安装包：https://www.postgresql.org/download/windows/
2. 运行安装向导，记住设置的超级用户密码。
3. 安装完成后，服务会自动注册并启动。

### 3.2 创建数据库与用户

使用 `psql` 连接到默认的 `postgres` 数据库，执行以下命令创建项目所需的数据库和用户：

```bash
# 以 postgres 超级用户连接（Linux/macOS）
sudo -u postgres psql          # Linux
psql -U $USER -d postgres      # macOS (Homebrew 默认当前用户为超级用户)
```

```sql
-- 创建数据库用户
CREATE USER stock WITH PASSWORD 'stock';

-- 创建数据库
CREATE DATABASE security_analyze OWNER stock ENCODING 'UTF8';

-- 授予权限（如需要创建 schema、表等）
GRANT ALL PRIVILEGES ON DATABASE security_analyze TO stock;

-- 退出
\q
```

### 3.3 验证连接

```bash
# 使用新建用户连接目标数据库
psql -h localhost -p 5432 -U stock -d security_analyze
# 密码: stock

# 连接成功后执行
\conninfo
-- 输出: You are connected to database "security_analyze" as user "stock" on host "localhost" ...
\dt
-- 输出: Did not find any relations.（尚未执行初始化脚本）
```

### 3.4 服务管理

| 平台 | 启动 | 停止 | 重启 | 查看状态 |
|------|------|------|------|----------|
| macOS (Homebrew) | `brew services start postgresql@16` | `brew services stop postgresql@16` | `brew services restart postgresql@16` | `brew services list` |
| Linux (systemd) | `sudo systemctl start postgresql` | `sudo systemctl stop postgresql` | `sudo systemctl restart postgresql` | `sudo systemctl status postgresql` |
| 手动 | `pg_ctl -D <data_dir> start` | `pg_ctl -D <data_dir> stop` | `pg_ctl -D <data_dir> restart` | `pg_isready` |

### 3.5 常用数据目录

| 平台 | 默认数据目录 |
|------|-------------|
| macOS (Homebrew) | `/opt/homebrew/var/postgresql@16` |
| Ubuntu/Debian | `/var/lib/postgresql/16/main` |
| CentOS/RHEL | `/var/lib/pgsql/16/data` |
| Windows | `C:\Program Files\PostgreSQL\16\data` |

---

## 4. 数据库初始化脚本

数据库初始化脚本位于 `backend/src/main/resources/db/migration/`，采用 Flyway 风格命名。**由于 PostgreSQL 独立部署，不再通过 Docker `initdb.d` 自动执行，需要手动按顺序运行。**

### 4.1 脚本清单

| 脚本文件 | 说明 |
|----------|------|
| `V1__create_company_table.sql` | 创建初始公司信息表（已废弃，被 V2 迁移） |
| `V2__create_company_and_security_tables.sql` | 拆分 company / company_security 表，并迁移旧数据 |
| `V2__create_collector_monitor_tables.sql` | 创建采集任务日志表 collector_task_log 与数据状态表 collector_data_status |
| `V3__create_financial_report_table.sql` | 创建财务报表表 financial_report，支持资产负债表/利润表/现金流量表 |
| `V4__create_stock_sync_status_table.sql` | 创建采集同步状态表 collector_stock_sync_status |
| `V5__create_user_table.sql` | 创建用户表 sys_user，含状态与角色枚举 |
| `V6__add_session_recovery.sql` | 扩展任务日志表 session_id 字段，新建逐票进度表 collector_task_progress |

### 4.2 手动执行初始化

**首次部署时，按版本号顺序逐一执行**：

```bash
# 切换到项目根目录
# 确保已创建数据库和用户，且 PostgreSQL 服务正在运行

# 方式 1：逐条执行（推荐，便于排查问题）
psql -h localhost -p 5432 -U stock -d security_analyze -f backend/src/main/resources/db/migration/V1__create_company_table.sql
psql -h localhost -p 5432 -U stock -d security_analyze -f backend/src/main/resources/db/migration/V2__create_company_and_security_tables.sql
psql -h localhost -p 5432 -U stock -d security_analyze -f backend/src/main/resources/db/migration/V2__create_collector_monitor_tables.sql
psql -h localhost -p 5432 -U stock -d security_analyze -f backend/src/main/resources/db/migration/V3__create_financial_report_table.sql
psql -h localhost -p 5432 -U stock -d security_analyze -f backend/src/main/resources/db/migration/V4__create_stock_sync_status_table.sql
psql -h localhost -p 5432 -U stock -d security_analyze -f backend/src/main/resources/db/migration/V5__create_user_table.sql
psql -h localhost -p 5432 -U stock -d security_analyze -f backend/src/main/resources/db/migration/V6__add_session_recovery.sql

# 方式 2：批量执行（适合重建环境）
for f in backend/src/main/resources/db/migration/V*.sql; do
    echo "Executing: $f"
    psql -h localhost -p 5432 -U stock -d security_analyze -f "$f"
done
```

### 4.3 验证初始化结果

```bash
psql -h localhost -p 5432 -U stock -d security_analyze -c "\dt"
```

期望输出应包含以下表：
- `company`
- `company_security`
- `financial_report`
- `collector_task_log`
- `collector_data_status`
- `collector_stock_sync_status`
- `collector_task_progress`
- `sys_user`

### 4.4 核心表结构

#### company（公司法人实体）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| unified_code | VARCHAR(50) UNIQUE | 统一社会信用代码（预留） |
| company_name | VARCHAR(200) | 公司全称 |
| short_name | VARCHAR(100) | 公司简称 |
| industry | VARCHAR(100) | 所属行业 |
| region | VARCHAR(50) | 地区（省份/直辖市） |
| establish_date | DATE | 成立日期 |
| registered_capital | DECIMAL(20,4) | 注册资本（万元） |

#### company_security（上市证券）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| company_id | BIGINT FK → company(id) | 所属公司 |
| stock_code | VARCHAR(20) UNIQUE | 股票代码 |
| stock_name | VARCHAR(100) | 证券简称 |
| market | VARCHAR(10) | 市场板块：SH / SZ / BJ / HK |
| security_type | VARCHAR(20) | 证券类型：A股 / B股 / H股 等 |
| listing_date | DATE | 上市日期 |
| listing_status | VARCHAR(20) | 上市状态：listed / suspended / delisted |

#### financial_report（财务报表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 主键 |
| stock_code | VARCHAR(20) | 股票代码 |
| report_date | DATE | 报告期 |
| report_type | VARCHAR(10) | 报告类型 |
| report_year | INTEGER | 报告年度 |
| 资产/负债/利润/现金流指标 | DECIMAL(20,4) | 详见脚本 |
| balance_sheet / profit_sheet / cash_flow_sheet | JSONB | 完整原始数据 |

### 4.5 初始化注意事项

1. **必须按版本顺序执行**：V1 → V2 → V3 → V4 → V5 → V6，因后续脚本依赖前置脚本创建的表或数据。
2. **重复执行安全性**：V2 以后的脚本多使用 `IF NOT EXISTS` 或 `IF EXISTS`，但 V2 包含数据迁移逻辑，不建议在已有数据的数据库上重复执行。
3. **如需重建数据库**：
   ```bash
   psql -U postgres -c "DROP DATABASE security_analyze;"
   psql -U postgres -c "CREATE DATABASE security_analyze OWNER stock;"
   # 然后重新按顺序执行所有迁移脚本
   ```
4. **生产环境**：建议引入 Flyway 或 Liquibase 进行正式的数据库版本管理，避免手动执行脚本。

---

## 5. 后端服务部署

### 5.1 目录结构

```
backend/
├── build.gradle              # Gradle 构建配置
├── gradlew / gradlew.bat     # Gradle Wrapper
├── settings.gradle
└── src/
    ├── main/java/com/example/securityanalyze/
    │   ├── SecurityAnalyzeApplication.java
    │   ├── config/           # 安全配置、CORS 等
    │   ├── company/          # 公司信息模块（api / application / domain / infrastructure）
    │   ├── auth/             # 认证模块
    │   └── user/             # 用户模块
    └── main/resources/
        ├── application.yml   # Spring 配置
        └── db/migration/     # 数据库初始化脚本
```

### 5.2 配置文件

`backend/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:security_analyze}
    username: ${DB_USER:stock}
    password: ${DB_PASSWORD:stock}
    driver-class-name: org.postgresql.Driver
```

### 5.3 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DB_HOST` | `localhost` | 数据库主机地址 |
| `DB_PORT` | `5432` | 数据库端口 |
| `DB_NAME` | `security_analyze` | 数据库名称 |
| `DB_USER` | `stock` | 数据库用户名 |
| `DB_PASSWORD` | `stock` | 数据库密码 |
| `JWT_SECRET` | — | JWT 签名密钥（生产环境必须配置） |

### 5.4 开发运行

```bash
cd backend

# 使用 Gradle Wrapper 启动（自动编译热加载）
./gradlew bootRun

# 或在 Windows 下
gradlew.bat bootRun
```

服务启动后将监听 `http://localhost:8080`。

### 5.5 生产构建

```bash
cd backend

# 构建可执行 JAR
./gradlew bootJar

# JAR 文件位置
ls build/libs/security-analyze-*.jar

# 运行 JAR（生产环境）
java -jar build/libs/security-analyze-0.0.1-SNAPSHOT.jar

# 或指定环境变量运行
DB_HOST=db.example.com DB_PASSWORD=secure_pass java -jar build/libs/*.jar
```

### 5.6 测试

```bash
./gradlew test
```

---

## 6. 前端服务部署

### 6.1 目录结构

```
frontend/
├── package.json
├── vite.config.ts            # Vite 配置，含开发代理
├── tsconfig*.json
├── index.html
└── src/
    ├── main.ts               # 入口：Pinia + Router + ElementPlus
    ├── App.vue
    ├── router/               # 路由定义
    ├── api/                  # HTTP API 封装（axios）
    ├── types/                # TypeScript 类型定义
    └── views/                # 页面视图
```

### 6.2 开发环境配置

`frontend/vite.config.ts` 开发服务器配置：

- 端口：`3000`
- API 代理：`/api` → `http://localhost:8080`

### 6.3 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | API 基础地址 |

创建 `.env.development` 或 `.env.production` 进行覆盖：

```bash
# frontend/.env.production
VITE_API_BASE_URL=https://api.example.com/api
```

### 6.4 开发运行

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器（热更新，代理 /api 到 localhost:8080）
npm run dev
```

开发服务器地址：`http://localhost:3000`

### 6.5 生产构建

```bash
cd frontend

# 安装依赖
npm install

# 生产构建（输出到 dist/）
npm run build

# 预览生产构建
npm run preview
```

### 6.6 生产部署（Nginx）

```nginx
server {
    listen 80;
    server_name example.com;
    root /var/www/security-analyze-frontend/dist;
    index index.html;

    # 前端静态资源
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 7. 数据采集服务部署

### 7.1 目录结构

```
collector/
├── pyproject.toml              # Poetry 配置
├── requirements.txt            # pip 备用依赖列表
├── main.py                     # 入口脚本
├── .env.example                # 环境变量模板
└── collector/
    ├── scheduler.py            # 定时调度器
    ├── monitor.py              # 采集监控
    ├── db/
    │   └── postgres.py         # PostgreSQL 连接封装
    ├── sources/
    │   └── akshare_source.py   # akshare 数据源封装
    └── tasks/
        ├── company_task.py     # 公司信息采集
        └── finance_task.py     # 财务报告采集
```

### 7.2 环境配置

创建 `collector/.env` 文件：

```bash
cp collector/.env.example collector/.env
```

内容示例：

```ini
DB_HOST=localhost
DB_PORT=5432
DB_NAME=security_analyze
DB_USER=stock
DB_PASSWORD=stock
```

### 7.3 安装依赖

**方式一：Poetry（推荐）**

```bash
cd collector
poetry install
```

**方式二：pip**

```bash
cd collector
pip install -r requirements.txt
```

依赖清单：

| 包名 | 版本 | 说明 |
|------|------|------|
| `psycopg[binary]` | ≥ 3.2.0 | PostgreSQL 驱动 |
| `akshare` | ≥ 1.15.0 | A 股数据源 |
| `schedule` | ≥ 1.2.0 | 定时调度 |
| `python-dotenv` | ≥ 1.0.0 | 环境变量读取 |
| `pydantic` | ≥ 2.0.0 | 数据校验 |

### 7.4 运行方式

```bash
cd collector

# 方式 1：启动调度器（常驻进程，当前无默认定时任务）
python main.py

# 方式 2：手动执行一次全量公司信息采集
python main.py --run-company

# 方式 3：按公司名称或股票代码采集
python main.py --company 贵州茅台
python main.py --company 600519

# 方式 4：手动执行一次全量财务报告采集（默认每批100家）
python main.py --run-finance

# 方式 5：按股票代码采集指定公司财务报告
python main.py --finance 600519

# 方式 6：财务报告采集（带年份范围与增量模式）
python main.py --finance 600519 --finance-start-year 2020 --finance-end-year 2024 --finance-incremental

# 方式 7：恢复指定 Session 的财务报告采集
python main.py --run-finance --finance-session-id <uuid>

# 方式 8：调整批次大小
python main.py --run-finance --finance-batch-size 50
```

### 7.5 生产部署建议

- **手动执行**：作为一次性任务运行，适合按需补数。
- **常驻调度**：作为 systemd 服务运行。
- **Cron Job**：通过 Linux cron 定时触发采集任务。

---

## 8. 服务启动顺序

### 8.1 首次部署启动顺序

```bash
# 步骤 1：启动 PostgreSQL 服务（所有服务依赖）
# macOS
brew services start postgresql@16

# Linux
sudo systemctl start postgresql

# 步骤 2：验证数据库就绪
pg_isready -h localhost -p 5432
# 输出: localhost:5432 - accepting connections

# 步骤 3：创建数据库与用户（如尚未创建）
psql -U postgres -c "CREATE USER stock WITH PASSWORD 'stock';"
psql -U postgres -c "CREATE DATABASE security_analyze OWNER stock;"

# 步骤 4：执行数据库初始化脚本
for f in backend/src/main/resources/db/migration/V*.sql; do
    echo "Executing: $f"
    psql -h localhost -p 5432 -U stock -d security_analyze -f "$f"
done

# 步骤 5：启动后端（终端 1）
cd backend
./gradlew bootRun

# 步骤 6：启动前端（终端 2）
cd frontend
npm run dev

# 步骤 7：执行初始数据采集（终端 3，可选）
cd collector
python main.py --run-company
```

### 8.2 服务依赖关系

```
PostgreSQL 服务 (必须先启动)
    ├── 数据库初始化 (首次部署必需)
    ├── 后端服务 (依赖数据库连接)
    │       └── 前端服务 (依赖后端 API，开发时通过 Vite 代理)
    └── 数据采集服务 (依赖数据库写入)
```

---

## 9. 生产环境部署建议

### 9.1 安全加固清单

| 项目 | 建议 |
|------|------|
| 数据库密码 | 修改默认密码 `stock`，使用强密码 |
| JWT Secret | 使用强随机字符串（≥ 256 bit），通过环境变量注入 |
| 管理员密码 | 修改默认密码 `admin123` |
| CORS | 配置严格的白名单，禁止 `*` 通配符 |
| 数据库连接 | 使用 SSL/TLS 加密传输；配置 `pg_hba.conf` 限制访问来源 |
| 网络隔离 | 数据库不暴露公网，仅内网访问；关闭不必要的端口 |

### 9.2 推荐部署架构

```
┌─────────────┐
│    Nginx    │  ← HTTPS 入口，静态资源托管，反向代理
│   (443/80)  │
└──────┬──────┘
       │
   ┌───┴───┐
   │       │
   ▼       ▼
┌──────┐ ┌─────────────┐
│ 前端  │ │  后端服务    │
│ dist │ │  (JAR 运行)  │
└──────┘ └──────┬──────┘
                │
                ▼
         ┌─────────────┐
         │  PostgreSQL │
         │  (内网访问)  │
         └─────────────┘
                ▲
                │
         ┌─────────────┐
         │ 数据采集服务 │
         │ (systemd /  │
         │  Cron Job)  │
         └─────────────┘
```

### 9.3 各组件生产部署命令

**数据库**

```bash
# 确保 PostgreSQL 服务已安装并运行
sudo systemctl enable postgresql
sudo systemctl start postgresql

# 创建数据库和用户
sudo -u postgres psql -c "CREATE USER stock WITH PASSWORD '<STRONG_PASSWORD>';"
sudo -u postgres psql -c "CREATE DATABASE security_analyze OWNER stock;"

# 执行初始化脚本
psql -h localhost -U stock -d security_analyze -f backend/src/main/resources/db/migration/V*.sql
```

**后端**

```bash
cd backend
./gradlew bootJar
# 通过 systemd / supervisor 管理 JAR 进程
java -Xms512m -Xmx2g -jar build/libs/*.jar
```

**前端**

```bash
cd frontend
npm install
npm run build
# 将 dist/ 目录部署到 Nginx / CDN
cp -r dist/ /var/www/security-analyze/
```

**数据采集**

```bash
cd collector
poetry install --no-dev
# 配置 cron 定时任务或常驻服务
python main.py --run-company
python main.py --run-finance --finance-incremental
```

---

## 10. 常见问题排查

### 10.1 数据库连接失败

```
Connection to localhost:5432 refused
```

- 检查 PostgreSQL 服务是否运行：`pg_isready` 或 `brew services list` / `sudo systemctl status postgresql`
- 检查端口是否被占用：`lsof -i :5432`
- 检查数据库和用户是否已创建：
  ```bash
  psql -U postgres -c "\l" | grep security_analyze
  psql -U postgres -c "\du" | grep stock
  ```
- 检查环境变量是否正确：`echo $DB_HOST $DB_PORT`
- 检查 `pg_hba.conf` 是否允许本地连接

### 10.2 前端 API 请求 404

- 检查后端是否启动：`curl http://localhost:8080/api/company`
- 检查 Vite 代理配置：`vite.config.ts` 中 `/api` 代理目标
- 检查 API 基础地址：`VITE_API_BASE_URL` 环境变量

### 10.3 采集模块导入失败

- 检查 Python 版本：`python3 --version`（需 ≥ 3.11）
- 检查依赖安装：`pip list | grep -E "psycopg|akshare|schedule"`
- 检查 `.env` 文件是否在 `collector/` 目录下

### 10.4 数据库初始化脚本执行失败

- 检查脚本执行顺序：必须按 V1 → V2 → V3 → V4 → V5 → V6 顺序执行
- 检查数据库用户权限：`GRANT ALL PRIVILEGES ON DATABASE security_analyze TO stock;`
- V2 脚本含数据迁移逻辑，在已有数据的数据库上重复执行可能报错，建议重建数据库后重新执行

### 10.5 端口冲突

| 服务 | 默认端口 | 修改方式 |
|------|---------|---------|
| PostgreSQL | 5432 | `postgresql.conf` 中 `port = 5432` |
| 后端 | 8080 | `application.yml` 中 `server.port` 或环境变量 |
| 前端开发服务器 | 3000 | `vite.config.ts` 中 `server.port` |

---

## 附录：一键启动脚本（开发环境）

```bash
#!/bin/bash
set -e

echo "=== 启动证券分析系统 ==="

# 1. 启动 PostgreSQL 服务
echo "[1/5] 启动 PostgreSQL 服务..."
if command -v brew &> /dev/null; then
    brew services start postgresql@16
elif command -v systemctl &> /dev/null; then
    sudo systemctl start postgresql
else
    echo "请手动启动 PostgreSQL 服务"
    exit 1
fi

# 2. 等待数据库就绪
echo "[2/5] 等待数据库就绪..."
until pg_isready -h localhost -p 5432 > /dev/null 2>&1; do
    sleep 2
done
echo "数据库已就绪"

# 3. 检查并执行初始化（可选：首次部署时取消注释）
# echo "[3/5] 执行数据库初始化..."
# for f in backend/src/main/resources/db/migration/V*.sql; do
#     psql -h localhost -p 5432 -U stock -d security_analyze -f "$f"
# done

# 4. 启动后端（后台）
echo "[4/5] 启动后端服务..."
cd backend
./gradlew bootRun &
cd ..

# 5. 启动前端
echo "[5/5] 启动前端服务..."
cd frontend
npm run dev &
cd ..

echo ""
echo "=== 服务启动完成 ==="
echo "前端: http://localhost:3000"
echo "后端: http://localhost:8080"
echo "数据库: localhost:5432"
```

---

*文档版本：2026-05-02*  
*适用系统版本：security-analyze v0.0.1-SNAPSHOT*  
*PostgreSQL 部署方式：独立部署（非 Docker）*
