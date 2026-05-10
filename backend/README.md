# 后端模块

基于 Gradle 9.x + Java 21 + Spring Boot 3.5 构建。

## 技术栈

- Gradle 9.4 (Kotlin DSL)
- Java 21
- Spring Boot 3.5
- Spring Data JPA
- PostgreSQL

## 常用命令

```bash
# 使用 Gradle Wrapper 构建
./gradlew build

# 运行应用
./gradlew bootRun

# 运行测试
./gradlew test
```

## 数据库

默认使用 PostgreSQL，需在本地启动 PostgreSQL 服务并创建数据库。
