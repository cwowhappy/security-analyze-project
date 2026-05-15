# Company Domain & Repository

> 21 nodes · cohesion 0.15

## Key Concepts

- **.update()** (30 connections) — `collector/src/data_collector/adapters/db_collection_task_repository.py`
- **JdbcUserRepository** (21 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.buildQuery()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.save()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.countWithConditions()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.findAllWithConditions()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.incrementFailedAttempts()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.lockUser()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.resetFailedAttempts()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.toEntity()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.unlock()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.updateDisplayNameAndRole()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.updateEmailVerified()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.updateLastLoginAt()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.updatePasswordExpiredAt()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.existsByEmail()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.existsByUsername()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.findByEmail()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.findById()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.findByUsername()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **.toDomain()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`

## Relationships

- [[Community 30]] (3 shared connections)
- [[Community 44]] (3 shared connections)
- [[Community 140]] (2 shared connections)
- [[Community 56]] (2 shared connections)
- [[Community 62]] (2 shared connections)
- [[Python Repositories]] (1 shared connections)
- [[Stock Repository Python]] (1 shared connections)
- [[Community 120]] (1 shared connections)
- [[Community 98]] (1 shared connections)
- [[Collection Task Domain]] (1 shared connections)
- [[Community 61]] (1 shared connections)
- [[Community 96]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- `collector/src/data_collector/adapters/db_collection_task_repository.py`

## Audit Trail

- EXTRACTED: 47 (55%)
- INFERRED: 38 (45%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*