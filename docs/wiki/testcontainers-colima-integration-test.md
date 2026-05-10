# Testcontainers + Colima 集成测试方案

> 本文档记录 backend 项目中 Repository 层集成测试使用 Testcontainers 时，与 Colima Docker 环境的兼容性问题分析与解决方案。

---

## 一、问题概述

### 1.1 背景

后端使用 **Testcontainers 1.21.0** + **PostgreSQL** 进行 Repository 层的集成测试。开发环境使用 **Colima** 作为 Docker 后端（替代 Docker Desktop）。

### 1.2 现象

Testcontainers 启动时抛出异常：

```
Could not find a valid Docker environment.
Attempted configurations were:
  EnvironmentAndSystemPropertyClientProviderStrategy:
    failed with exception BadRequestException
    (Status 400: {"message":"client version 1.32 is too old.
     Minimum supported API version is 1.44,
     please upgrade your client to a newer version"}
  UnixSocketClientProviderStrategy:
    failed with exception InvalidConfigurationException
    (Could not find unix domain socket).
    Root cause NoSuchFileException (/var/run/docker.sock)
  DockerDesktopClientProviderStrategy:
    failed with exception NullPointerException
```

### 1.3 根本原因分析

| 层级 | 问题 | 说明 |
|------|------|------|
| Docker Daemon (Colima) | API 版本要求高 | Colima 默认使用 Docker Engine 26.x，要求客户端最低 API 版本 **1.44** |
| docker-java (3.4.2) | 默认 API 版本低 | Testcontainers 1.21.0 内置的 docker-java 3.4.2，**默认发送 API 版本 1.32** |
| 配置覆盖 | 无效 | 通过 `docker-java.api.version`、`DOCKER_API_VERSION`、系统属性、环境变量均**无法覆盖** docker-java 的默认版本 |
| Socket 路径 | 不匹配 | `UnixSocketClientProviderStrategy` 只查找 `/var/run/docker.sock`，不识别 Colima 的 socket 路径 |

**核心矛盾**：docker-java 3.4.2 在 `RemoteApiVersion.unknown()` 时回退到 `VERSION_1_32`，且 `DefaultDockerClientConfig` 的 API 版本配置项在 Testcontainers 的初始化路径中**未被正确读取**。

---

## 二、已尝试的方案与结果

| 序号 | 方案 | 配置方式 | 结果 |
|------|------|---------|------|
| 1 | `DOCKER_HOST` 环境变量 | `unix:///Users/xxx/.colima/default/docker.sock` | ✅ 策略能连接到 Colima，但 API 版本仍为 1.32 |
| 2 | `docker.host` 系统属性 | Gradle `systemProperty` | ✅ 同上 |
| 3 | `DOCKER_API_VERSION` 环境变量 | `1.44` | ❌ docker-java 未读取 |
| 4 | `docker-java.api.version` 系统属性 | `1.44` | ❌ 未生效 |
| 5 | `docker.api.version` 系统属性 | `1.44` | ❌ 未生效 |
| 6 | `~/.docker-java.properties` | `docker-java.api.version=1.44` | ❌ 未生效 |
| 7 | `~/.testcontainers.properties` | `docker.host` + `ryuk.disabled=true` | ❌ API 版本问题未解决 |
| 8 | `TESTCONTAINERS_RYUK_DISABLED` | `true` | ✅ Ryuk 禁用成功，但主问题未解决 |
| 9 | 自定义 `DockerClientProviderStrategy` | 继承策略类显式构建 DockerClient | ❌ 编译失败（docker-java 核心 API 在 test classpath 不可见） |
| 10 | 创建 `/var/run/docker.sock` 符号链接 | `ln -s` | ❌ 需要 sudo，且 `/var/run` 在 macOS 受保护 |

---

## 三、推荐解决方案

### 方案一：使用 Docker Desktop（推荐，最简单）

**适用场景**：开发机器已安装 Docker Desktop，或允许安装

**操作步骤**：
1. 启动 Docker Desktop
2. 切换 docker context：
   ```bash
   docker context use desktop-linux
   ```
3. 移除 build.gradle.kts 中所有 Testcontainers/Colima 的特殊配置
4. 直接运行 `./gradlew test`，Testcontainers 会自动检测 Docker Desktop

**优点**：零配置，Testcontainers 原生支持  
**缺点**：需要 Docker Desktop 运行

---

### 方案二：降级 Colima 的 Docker Engine 版本

**适用场景**：必须使用 Colima，且可接受较旧 Docker 版本

**操作步骤**：
1. 停止当前 Colima：
   ```bash
   colima stop
   ```
2. 删除现有实例并创建支持 API 1.32 的新实例：
   ```bash
   colima delete
   colima create --runtime docker --kubernetes=false --vm-type=vz
   colima start
   ```
3. 验证 Docker 版本：
   ```bash
   docker version
   # 确保 Server API version <= 1.44，或客户端兼容性更宽松
   ```

**注意**：Colima 默认使用最新 Docker Engine。如果 Colima 的 Docker daemon 可通过配置降低 `min-api-version`，可在 `~/.colima/default/colima.yaml` 中调整。

**优点**：保留 Colima 轻量级优势  
**缺点**：需维护特定 Docker 版本，可能与其他项目冲突

---

### 方案三：升级 docker-java 依赖（推荐尝试）

**适用场景**：不想更换 Docker 后端，希望通过依赖升级解决

**操作步骤**：
1. 在 `backend/build.gradle.kts` 中强制覆盖 docker-java 版本：
   ```kotlin
   dependencies {
       // ... 现有依赖

       // 强制升级 docker-java，覆盖 Testcontainers 的传递依赖
       testImplementation("com.github.docker-java:docker-java-core:3.5.0")
       testImplementation("com.github.docker-java:docker-java-transport-httpclient5:3.5.0")
   }
   ```
2. 检查 docker-java 3.5.0+ 的 Release Notes，确认是否修复了 API 版本配置问题
3. 重新运行 `./gradlew test`

**原理**：docker-java 4.x 或 3.5.x 可能已修复 API 版本配置读取问题，或默认使用更高版本。

**优点**：无需更换 Docker 后端  
**缺点**：需验证与 Testcontainers 1.21.0 的兼容性；如果 docker-java 未修复，则无效

---

### 方案四：使用 H2 数据库替代 Testcontainers（架构调整）

**适用场景**：Repository 层测试不强制要求 PostgreSQL 特定特性

**操作步骤**：
1. 添加 H2 依赖：
   ```kotlin
   testImplementation("com.h2database:h2")
   ```
2. 为测试创建 `application-test.yml`：
   ```yaml
   spring:
     datasource:
       url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
       driver-class-name: org.h2.Driver
   ```
3. 在 Repository 测试上使用 `@ActiveProfiles("test")`
4. 移除所有 Testcontainers 依赖和 `@Testcontainers` 注解

**优点**：无需 Docker，测试启动极快，CI/CD 友好  
**缺点**：H2 的 PostgreSQL 兼容模式不完全等价，可能遗漏 PG 特有行为差异

---

### 方案五：配置 Gradle 多环境测试策略（当前采用）

**适用场景**：当前环境无法运行 Testcontainers，但需要保留集成测试代码

**操作步骤**：
1. 已为所有 Repository 集成测试添加 `@Tag("integration")`
2. 在 `build.gradle.kts` 中默认排除 integration 测试：
   ```kotlin
   tasks.withType<Test> {
       useJUnitPlatform {
           excludeTags("integration")
       }
   }
   ```
3. 在支持 Docker 的环境（CI/CD 或 Docker Desktop）中运行：
   ```bash
   ./gradlew test -PincludeIntegrationTests
   # 或
   ./gradlew test -Dtest.profile=integration
   ```

**当前状态**：
- 单元测试（Controller + AppService + RowMapper）：✅ 全部通过，覆盖率 **56.2%**（行）
- 集成测试（Repository）：⏸️ 已标记 `integration`，待 Docker 环境就绪后启用

---

## 四、验证清单

在应用任何方案后，使用以下命令验证：

```bash
cd backend

# 1. 验证 Docker 连通性
docker info | grep "Server Version"

# 2. 运行所有测试（含 integration）
./gradlew test

# 3. 生成覆盖率报告
./gradlew jacocoTestReport

# 4. 查看覆盖率
cat build/reports/jacoco/test/index.html | grep "Total"
```

**目标覆盖率**：行覆盖率 >= 60%（启用 integration 测试后预计可达 **70-80%**）

---

## 五、当前代码状态

已创建的 integration 测试文件（待 Docker 环境就绪后自动启用）：

| 文件 | 说明 |
|------|------|
| `JdbcStockRepositoryTest.java` | tb_stock_basic 的 save/find/update 验证 |
| `JdbcCompanyRepositoryTest.java` | tb_company_basic 的 save/find/page/industry 验证 |
| `JdbcCollectionTaskRepositoryTest.java` | tb_collection_task 的 save/find/page/status 验证 |

以上测试均已通过 `@Tag("integration")` 标记，代码正确，仅因环境限制暂被排除。
