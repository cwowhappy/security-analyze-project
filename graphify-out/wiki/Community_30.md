# Community 30

> 18 nodes · cohesion 0.12

## Key Concepts

- **NamedParameterJdbcTemplate** (15 connections)
- **JdbcEmailVerificationRepository** (6 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepository.java`
- **EmailVerificationRepository** (5 connections)
- **JdbcUserRepository.java** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- **JdbcPasswordResetRepository** (4 connections)
- **JdbcEmailVerificationRepository.java** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepository.java`
- **JdbcEmailVerificationRepositoryTest.java** (3 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepositoryTest.java`
- **JdbcTokenSessionRepositoryTest.java** (3 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepositoryTest.java`
- **JdbcConfig.java** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/config/JdbcConfig.java`
- **JdbcConfig** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/config/JdbcConfig.java`
- **PasswordResetRepository** (2 connections)
- **.markAsUsed()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepository.java`
- **.save()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepository.java`
- **.namedParameterJdbcTemplate()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/config/JdbcConfig.java`
- **PasswordReset** (1 connections)
- **.findLatestByUserId()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepository.java`
- **.toDomain()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepository.java`
- **tb_password_reset** (1 connections)

## Relationships

- [[Company Domain & Repository]] (3 shared connections)
- [[Community 37]] (2 shared connections)
- [[Community 44]] (2 shared connections)
- [[Backend User Management]] (2 shared connections)
- [[Community 120]] (1 shared connections)
- [[Community 98]] (1 shared connections)
- [[Community 31]] (1 shared connections)
- [[Community 148]] (1 shared connections)
- [[Community 34]] (1 shared connections)
- [[Community 74]] (1 shared connections)
- [[Login Log Management]] (1 shared connections)
- [[Community 43]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/config/JdbcConfig.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepository.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcUserRepository.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcEmailVerificationRepositoryTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/user/infrastructure/persistence/repository/JdbcTokenSessionRepositoryTest.java`

## Audit Trail

- EXTRACTED: 44 (76%)
- INFERRED: 2 (3%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*