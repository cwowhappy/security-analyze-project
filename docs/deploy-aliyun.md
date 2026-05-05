# 证券分析系统 — 阿里云生产环境部署文档

> **部署目标**：将证券分析系统（前端 Vue 3 + 后端 Spring Boot + Python 采集模块）部署至阿里云 ECS（CentOS/Ubuntu）+ 阿里云 RDS PostgreSQL 18。
> 
> **前置条件**：ECS 已安装 Nginx；代码托管在 Gitee。

---

## 一、环境概览

| 组件 | 环境/版本 | 说明 |
|------|-----------|------|
| 服务器 | 阿里云 ECS | 已部署 Nginx，建议配置 ≥ 2C4G |
| 数据库 | 阿里云 RDS PostgreSQL 18 | 与 ECS 处于同一 VPC，内网互通 |
| 代码仓库 | Gitee | 私有仓库，需配置 SSH Key 或 HTTPS 凭证 |
| 后端 | Java 21 + Spring Boot 3.5.x | 监听端口 `8080` |
| 前端 | Vue 3 + Vite | 构建为静态资源，由 Nginx 托管 |
| 采集模块 | Python 3.11+ | 直接连接 RDS，不暴露 HTTP 端口 |

---

## 二、ECS 基础环境准备

### 2.1 系统依赖安装

```bash
# 更新系统包（以 CentOS 为例，Ubuntu 请使用 apt）
sudo yum update -y

# 安装 Git
sudo yum install -y git

# 安装 Java 21（Amazon Corretto 或 Oracle JDK）
sudo rpm --import https://yum.corretto.aws/corretto.key
sudo curl -Lo /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo
sudo yum install -y java-21-amazon-corretto-devel

# 验证
java -version   # 应显示 openjdk version "21.xxx"

# 安装 Node.js 20 LTS
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo yum install -y nodejs
node -v   # v20.x.x
npm -v    # 10.x.x

# 安装 Python 3.11+（如系统自带低于 3.11，需手动编译或使用 conda）
python3 --version   # >= 3.11

# 安装 Poetry（推荐）
curl -sSL https://install.python-poetry.org | python3 -
# 将 Poetry 加入 PATH
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
poetry --version
```

### 2.2 防火墙与安全组

| 端口 | 用途 | 开放范围 |
|------|------|----------|
| 22 | SSH | 办公 IP 白名单 |
| 80 | HTTP（Nginx） | 0.0.0.0/0 |
| 443 | HTTPS（Nginx） | 0.0.0.0/0 |
| 8080 | 后端 API | **仅本机/内网**（通过 Nginx 反向代理暴露） |

> **安全提示**：生产环境禁止将 `8080` 直接暴露在公网，所有 `/api/**` 请求应通过 Nginx 反向代理转发。

---

## 三、RDS 数据库初始化

### 3.1 创建数据库与用户

通过阿里云 RDS 控制台或 `psql` 连接 RDS 外网/内网地址：

```bash
# 连接 RDS（将 <rds-endpoint> 替换为实际内网地址）
psql -h <rds-endpoint>.pg.rds.aliyuncs.com -p 5432 -U postgres

-- 创建数据库（名称需与后端配置一致）
CREATE DATABASE db_security_analyze OWNER postgres ENCODING 'UTF8';

-- 创建专用业务用户（可选，但推荐）
CREATE USER user_security_analyze WITH PASSWORD 'YourStrongPassword';
GRANT ALL PRIVILEGES ON DATABASE db_security_analyze TO user_security_analyze;
```

### 3.2 执行数据库迁移脚本

> **注意**：生产环境 `flyway.enabled=false`，需**手动按版本顺序**执行 SQL 脚本。

```bash
# 1. 克隆代码到 ECS（或本地执行后确认）
git clone https://gitee.com/<your-org>/security-analyze-project.git /opt/security-analyze
cd /opt/security-analyze

# 2. 按顺序执行迁移脚本（使用有权限的 RDS 账号）
for f in backend/src/main/resources/db/migration/V*.sql; do
    echo "Executing $f ..."
    psql -h <rds-endpoint>.pg.rds.aliyuncs.com -p 5432 \
         -U user_security_analyze -d db_security_analyze -f "$f"
done
```

当前迁移脚本清单（请随版本迭代更新）：
- `V1__baseline.sql` — 基础表（公司、证券、用户、财务报表、采集日志等）
- `V2__industry_classification.sql` — 行业分类体系
- `V3__index_module.sql` — 指数与 ETF 模块
- `V4__index_core_flag.sql` — 核心指数标记

### 3.3 初始化管理员账号

```sql
-- 默认管理员（首次部署后务必修改密码）
INSERT INTO sys_user (username, password_hash, real_name, status, role)
VALUES ('admin', '$2a$10$...', '系统管理员', 'APPROVED', 'ADMIN');
```

> **安全警告**：生产环境必须替换默认密码 `admin123`，并使用强哈希值存储。

---

## 四、项目部署

### 4.1 拉取代码

```bash
mkdir -p /opt/security-analyze
cd /opt/security-analyze
git clone https://gitee.com/<your-org>/security-analyze-project.git .
```

如需使用 SSH 拉取：
```bash
git clone git@gitee.com:<your-org>/security-analyze-project.git .
```

---

### 4.2 后端部署（Spring Boot）

#### 4.2.1 构建可执行 JAR

```bash
cd /opt/security-analyze/backend

# 赋予 Gradle Wrapper 执行权限
chmod +x gradlew

# 生产构建（跳过测试以加速，首次部署建议运行测试）
./gradlew bootJar -x test

# 产物位置：build/libs/security-analyze-0.0.1-SNAPSHOT.jar
ls -lh build/libs/
```

#### 4.2.2 配置环境变量

创建 `/opt/security-analyze/backend/.env`：

```bash
# 数据库连接（使用 RDS 内网地址）
export DB_HOST=<rds-endpoint>.pg.rds.aliyuncs.com
export DB_PORT=5432
export DB_NAME=db_security_analyze
export DB_USER=user_security_analyze
export DB_PASSWORD=YourStrongPassword

# JWT Secret（生产环境必须替换为强随机字符串，≥ 256 bit）
export JWT_SECRET=ChangeThisToARandomStringWithAtLeast32Chars

# 日志目录（确保有写入权限）
export LOG_PATH=/var/log/security-analyze
```

#### 4.2.3 Systemd 服务化

创建 `/etc/systemd/system/security-analyze-backend.service`：

```ini
[Unit]
Description=Security Analyze Backend Service
After=network.target

[Service]
Type=simple
User=security
Group=security
WorkingDirectory=/opt/security-analyze/backend
EnvironmentFile=/opt/security-analyze/backend/.env
ExecStart=/usr/bin/java -jar /opt/security-analyze/backend/build/libs/security-analyze-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

启动并启用开机自启：

```bash
# 创建运行用户（如无）
sudo useradd -r -s /bin/false security || true

# 设置目录权限
sudo chown -R security:security /opt/security-analyze/backend
sudo mkdir -p /var/log/security-analyze
sudo chown security:security /var/log/security-analyze

# 重载 systemd 并启动
sudo systemctl daemon-reload
sudo systemctl enable security-analyze-backend
sudo systemctl start security-analyze-backend
sudo systemctl status security-analyze-backend
```

验证后端运行：
```bash
curl -s http://localhost:8080/api/health || echo "请检查日志: journalctl -u security-analyze-backend -f"
```

---

### 4.3 前端部署（Vue 3 + Nginx）

#### 4.3.1 构建生产包

```bash
cd /opt/security-analyze/frontend

# 安装依赖
npm install

# 生产构建
npm run build

# 产物输出到 dist/ 目录
ls -ld dist/
```

#### 4.3.2 Nginx 配置

编辑 Nginx 站点配置（通常为 `/etc/nginx/conf.d/security-analyze.conf`）：

```nginx
server {
    listen 80;
    server_name your-domain.com;   # 替换为实际域名或 ECS 公网 IP

    # 前端静态资源
    location / {
        root /opt/security-analyze/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;   # 支持 Vue Router History 模式
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # 日志
    access_log /var/log/nginx/security-analyze-access.log;
    error_log /var/log/nginx/security-analyze-error.log;
}
```

检查配置并重载 Nginx：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

### 4.4 数据采集模块部署（Python）

#### 4.4.1 安装依赖

```bash
cd /opt/security-analyze/collector

# 方式一：Poetry（推荐）
poetry install --no-dev

# 方式二：pip（备用）
pip install -r requirements.txt
```

#### 4.4.2 配置环境变量

创建 `/opt/security-analyze/collector/.env`：

```bash
DB_HOST=<rds-endpoint>.pg.rds.aliyuncs.com
DB_PORT=5432
DB_NAME=db_security_analyze
DB_USER=user_security_analyze
DB_PASSWORD=YourStrongPassword

# 连接池
DB_POOL_MIN_SIZE=1
DB_POOL_MAX_SIZE=5

# 采集重试
SOURCE_MAX_RETRIES=3
SOURCE_RETRY_DELAY=2.0
SOURCE_RETRY_BACKOFF=2.0

# 财务采集
FINANCE_BATCH_SIZE=100
FINANCE_MAX_WORKERS=3
FINANCE_BATCH_CONCURRENT_WORKERS=3
```

#### 4.4.3 运行方式（二选一）

**方式 A：Systemd 常驻调度器（推荐）**

创建 `/etc/systemd/system/security-analyze-collector.service`：

```ini
[Unit]
Description=Security Analyze Data Collector Scheduler
After=network.target

[Service]
Type=simple
User=security
Group=security
WorkingDirectory=/opt/security-analyze/collector
ExecStart=/usr/local/bin/poetry run python main.py \
    --scheduler-cron-company "0 9 * * *" \
    --scheduler-cron-finance "0 2 * * 0" \
    --scheduler-cron-industry "0 3 * * 1" \
    --scheduler-cron-index-basic "0 4 * * 1" \
    --scheduler-cron-index-history "0 5 * * 1" \
    --scheduler-cron-etf-basic "0 6 * * 1"
Restart=on-failure
RestartSec=30
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

启动：
```bash
sudo systemctl daemon-reload
sudo systemctl enable security-analyze-collector
sudo systemctl start security-analyze-collector
sudo journalctl -u security-analyze-collector -f
```

**方式 B：Crontab 定时任务（轻量）**

```bash
sudo crontab -e -u security

# 每日 09:00 执行公司信息采集
0 9 * * * cd /opt/security-analyze/collector && /usr/local/bin/poetry run python main.py --run-company >> /var/log/collector-company.log 2>&1

# 每周日 02:00 执行财务报告采集
0 2 * * 0 cd /opt/security-analyze/collector && /usr/local/bin/poetry run python main.py --run-finance >> /var/log/collector-finance.log 2>&1

# 每周一 03:00 执行行业分类同步
0 3 * * 1 cd /opt/security-analyze/collector && /usr/local/bin/poetry run python main.py --sync-industry >> /var/log/collector-industry.log 2>&1
```

---

## 五、首次数据初始化

系统部署完成后，需手动执行首次全量数据采集：

```bash
cd /opt/security-analyze/collector

# 1. 采集全部上市公司基础信息
poetry run python main.py --run-company

# 2. 同步行业分类体系
poetry run python main.py --sync-industry

# 3. 采集全部历史财务报告（首次可能耗时较长，建议分批或后台执行）
nohup poetry run python main.py --run-finance > /var/log/collector-init-finance.log 2>&1 &

# 4. 采集指数与 ETF 数据
poetry run python main.py --run-index-basic
poetry run python main.py --run-etf-basic
```

---

## 六、SSL/HTTPS 配置（推荐）

通过阿里云 SSL 证书或 Let's Encrypt 配置 HTTPS：

```bash
# 以 Certbot 为例
sudo yum install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com

# 自动续期已默认启用，可通过以下命令测试
sudo certbot renew --dry-run
```

Nginx 配置更新后，确保持久化重载：
```bash
sudo systemctl enable nginx
```

---

## 七、运维与监控

### 7.1 常用运维命令

```bash
# 查看后端日志
sudo journalctl -u security-analyze-backend -f

# 查看采集模块日志
sudo journalctl -u security-analyze-collector -f

# 重启服务
sudo systemctl restart security-analyze-backend
sudo systemctl restart security-analyze-collector

# 查看 Nginx 访问日志
sudo tail -f /var/log/nginx/security-analyze-access.log

# 手动执行指定股票数据采集
poetry run python main.py --company 600519
poetry run python main.py --finance 600519 --finance-start-year 2020 --finance-end-year 2024
```

### 7.2 健康检查

| 检查项 | 命令/方式 |
|--------|-----------|
| 后端服务 | `curl http://localhost:8080/api/health` |
| Nginx | `curl -I http://your-domain.com` |
| 数据库连通性 | `psql -h <rds> -U user_security_analyze -d db_security_analyze -c "SELECT 1;"` |
| 磁盘空间 | `df -h` |
| 采集进度 | 查询 `collector_task_log` 表 |

### 7.3 备份策略

```bash
# RDS 自动备份：在阿里云控制台开启自动备份策略（建议每日）

# 手动逻辑备份（如需本地保留）
pg_dump -h <rds-endpoint>.pg.rds.aliyuncs.com -p 5432 \
        -U user_security_analyze -d db_security_analyze \
        -F c -f /backup/security-analyze-$(date +%Y%m%d).dump
```

---

## 八、版本升级流程

```bash
cd /opt/security-analyze

# 1. 拉取最新代码
git pull origin main

# 2. 执行新增数据库迁移（如有）
for f in backend/src/main/resources/db/migration/V*.sql; do
    psql -h <rds-endpoint>.pg.rds.aliyuncs.com -p 5432 \
         -U user_security_analyze -d db_security_analyze -f "$f"
done

# 3. 重新构建并重启后端
cd backend && ./gradlew bootJar -x test
sudo systemctl restart security-analyze-backend

# 4. 重新构建并部署前端
cd ../frontend && npm install && npm run build
sudo systemctl reload nginx

# 5. 重启采集模块（如代码有变更）
sudo systemctl restart security-analyze-collector
```

---

## 九、安全 checklist

- [ ] RDS 数据库密码为强密码，且未硬编码在代码中。
- [ ] `JWT_SECRET` 已替换为 ≥ 32 字节的随机字符串。
- [ ] 默认管理员密码 `admin123` 已修改。
- [ ] ECS 安全组仅开放 80/443 至公网，`8080` 仅限内网/本机访问。
- [ ] Nginx 已配置 HTTPS（SSL/TLS）。
- [ ] RDS 白名单限制仅允许 ECS 内网 IP 访问。
- [ ] 服务器已启用自动安全更新（`yum-cron` 或 `unattended-upgrades`）。
- [ ] 日志文件已配置定期轮转（`logrotate`）。

---

## 十、附录：目录结构参考

```
/opt/security-analyze/
├── backend/
│   ├── build/libs/security-analyze-0.0.1-SNAPSHOT.jar
│   └── .env                          # 后端环境变量
├── frontend/
│   └── dist/                         # Nginx 托管目录
├── collector/
│   ├── main.py
│   └── .env                          # 采集模块环境变量
└── docs/
    └── deploy-aliyun.md              # 本文档
```

---

*文档版本：v1.0*  
*最后更新：2026-05-05*
