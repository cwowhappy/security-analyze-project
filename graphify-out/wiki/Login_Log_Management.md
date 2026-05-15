# Login Log Management

> 41 nodes · cohesion 0.06

## Key Concepts

- **JdbcLoginLogRepository** (8 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepository.java`
- **AdminLoginLogController** (7 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogController.java`
- **LoginLogRepository** (7 connections)
- **JdbcLoginLogRepositoryTest** (6 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepositoryTest.java`
- **AdminLogAppService** (4 connections)
- **AdminLogAppServiceImpl.java** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/impl/AdminLogAppServiceImpl.java`
- **AdminLoginLogControllerTest.java** (4 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogControllerTest.java`
- **JdbcLoginLogRepositoryTest.java** (4 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepositoryTest.java`
- **AdminLoginLogControllerTest** (4 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogControllerTest.java`
- **AdminLogAppServiceImpl** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/impl/AdminLogAppServiceImpl.java`
- **.buildQuery()** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepository.java`
- **AdminLoginLogController.java** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogController.java`
- **JdbcLoginLogRepository.java** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepository.java`
- **AdminLogAppServiceImplTest.java** (3 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AdminLogAppServiceImplTest.java`
- **.assertAdmin()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogController.java`
- **.exportLogs()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogController.java`
- **.listLogs()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogController.java`
- **.toCsv()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogController.java`
- **AdminLogAppServiceImplTest** (3 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AdminLogAppServiceImplTest.java`
- **LoginLogServiceImplTest.java** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/LoginLogServiceImplTest.java`
- **.escapeCsv()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogController.java`
- **.countByConditions()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepository.java`
- **.findAllByConditions()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepository.java`
- **.findByConditions()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepository.java`
- **.save()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepository.java`
- *... and 16 more nodes in this community*

## Relationships

- [[Community 34]] (4 shared connections)
- [[Community 37]] (2 shared connections)
- [[Community 30]] (1 shared connections)
- [[Community 32]] (1 shared connections)
- [[Global Exception Handling]] (1 shared connections)
- [[Community 164]] (1 shared connections)
- [[Company Domain & Repository]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogController.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/impl/AdminLogAppServiceImpl.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepository.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminLoginLogControllerTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AdminLogAppServiceImplTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/LoginLogServiceImplTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcLoginLogRepositoryTest.java`

## Audit Trail

- EXTRACTED: 107 (98%)
- INFERRED: 2 (2%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*