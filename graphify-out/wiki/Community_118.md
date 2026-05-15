# Community 118

> 6 nodes · cohesion 0.33

## Key Concepts

- **.getUserIdFromToken()** (8 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/config/JwtTokenProvider.java`
- **.shouldReturnCurrentUserWhenTokenValid()** (4 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AuthControllerTest.java`
- **.logout()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/impl/AuthAppServiceImpl.java`
- **.shouldRecordLogoutLog()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **AuthContextHelper** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/support/AuthContextHelper.java`
- **.getCurrentUser()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/support/AuthContextHelper.java`

## Relationships

- [[Community 40]] (3 shared connections)
- [[Community 119]] (1 shared connections)
- [[Global Exception Handling]] (1 shared connections)
- [[RowMapper Unit Tests]] (1 shared connections)
- [[Community 55]] (1 shared connections)
- [[Community 72]] (1 shared connections)
- [[Auth Service & JWT]] (1 shared connections)
- [[Community 32]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/config/JwtTokenProvider.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/support/AuthContextHelper.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/impl/AuthAppServiceImpl.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AuthControllerTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`

## Audit Trail

- EXTRACTED: 7 (35%)
- INFERRED: 13 (65%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*