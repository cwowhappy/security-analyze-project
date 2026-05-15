# Collection Task Domain

> 41 nodes · cohesion 0.06

## Key Concepts

- **TaskType** (15 connections) — `collector/src/data_collector/core/domain/collection_task.py`
- **JdbcCollectionTaskRepository** (8 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java`
- **TestCollectionTask** (7 connections) — `collector/tests/unit/test_collection_task_domain.py`
- **collection_task.py** (6 connections) — `collector/src/data_collector/core/domain/collection_task.py`
- **TaskStatus** (6 connections) — `collector/src/data_collector/core/domain/collection_task.py`
- **CollectionTaskAppServiceImpl** (6 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/impl/CollectionTaskAppServiceImpl.java`
- **JdbcCollectionTaskRepositoryTest** (6 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java`
- **CollectionTaskAppServiceTest** (6 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/collection/application/service/CollectionTaskAppServiceTest.java`
- **CollectionTaskControllerTest** (4 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CollectionTaskControllerTest.java`
- **.buildTask()** (4 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java`
- **Enum** (3 connections)
- **.save()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java`
- **.shouldCreateTaskWhenRequestValid()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CollectionTaskControllerTest.java`
- **.shouldReturnTasksWhenListTasks()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CollectionTaskControllerTest.java`
- **.shouldReturnTaskWhenFoundById()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CollectionTaskControllerTest.java`
- **.createTask()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/impl/CollectionTaskAppServiceImpl.java`
- **.toDTO()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/impl/CollectionTaskAppServiceImpl.java`
- **.toDomain()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java`
- **.toEntity()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java`
- **.shouldFindTasksByPageAndStatus()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java`
- **.shouldSaveAndFindTaskById()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java`
- **.shouldUpdateTaskStatus()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java`
- **.setUp()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/collection/application/service/CollectionTaskAppServiceTest.java`
- **.shouldCreateTaskAndReturnId()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/collection/application/service/CollectionTaskAppServiceTest.java`
- **test_collection_task_domain.py** (1 connections) — `collector/tests/unit/test_collection_task_domain.py`
- *... and 16 more nodes in this community*

## Relationships

- [[Community 34]] (7 shared connections)
- [[Community 33]] (4 shared connections)
- [[Financial Report Service]] (2 shared connections)
- [[Global Exception Handling]] (1 shared connections)
- [[Company Domain & Repository]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/collection/application/service/impl/CollectionTaskAppServiceImpl.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepository.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/collection/application/service/CollectionTaskAppServiceTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/collection/infrastructure/persistence/repository/JdbcCollectionTaskRepositoryTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CollectionTaskControllerTest.java`
- `collector/src/data_collector/core/domain/collection_task.py`
- `collector/tests/unit/test_collection_task_domain.py`

## Audit Trail

- EXTRACTED: 88 (77%)
- INFERRED: 27 (23%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*