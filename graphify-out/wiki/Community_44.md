# Community 44

> 14 nodes · cohesion 0.15

## Key Concepts

- **TokenBlacklistServiceImplTest** (6 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/impl/TokenBlacklistServiceImplTest.java`
- **JdbcTokenSessionRepository** (6 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepository.java`
- **TokenSessionRepository** (5 connections)
- **JdbcTokenSessionRepository.java** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepository.java`
- **TokenBlacklistServiceImplTest.java** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/impl/TokenBlacklistServiceImplTest.java`
- **.deleteByTokenHash()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepository.java`
- **.deleteExpiredSessions()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepository.java`
- **.save()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepository.java`
- **.shouldDeleteTokenHashOnRevoke()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/impl/TokenBlacklistServiceImplTest.java`
- **.shouldGenerateSameHashForSameToken()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/impl/TokenBlacklistServiceImplTest.java`
- **.shouldRecordTokenHash()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/impl/TokenBlacklistServiceImplTest.java`
- **.shouldReturnFalseWhenTokenNotExists()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/impl/TokenBlacklistServiceImplTest.java`
- **.shouldReturnTrueWhenTokenExists()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/impl/TokenBlacklistServiceImplTest.java`
- **.existsByTokenHash()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepository.java`

## Relationships

- [[Company Domain & Repository]] (3 shared connections)
- [[Community 30]] (2 shared connections)
- [[Community 121]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepository.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/impl/TokenBlacklistServiceImplTest.java`

## Audit Trail

- EXTRACTED: 31 (91%)
- INFERRED: 3 (9%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*