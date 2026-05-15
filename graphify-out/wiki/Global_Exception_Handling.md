# Global Exception Handling

> 76 nodes · cohesion 0.05

## Key Concepts

- **.success()** (39 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/response/ApiResponse.java`
- **AuthController** (14 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AuthController.java`
- **.error()** (14 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/response/ApiResponse.java`
- **AdminUserController** (9 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminUserController.java`
- **GlobalExceptionHandler** (8 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandler.java`
- **FinancialAnalysisController** (8 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/interfaces/rest/controller/FinancialAnalysisController.java`
- **GlobalExceptionHandlerTest** (7 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandlerTest.java`
- **.assertAdmin()** (6 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminUserController.java`
- **.getClientIp()** (5 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AuthController.java`
- **.getCurrentUser()** (5 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AuthController.java`
- **CompanyController** (5 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CompanyController.java`
- **StockController** (5 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/StockController.java`
- **ApiResponseTest** (5 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/response/ApiResponseTest.java`
- **.handleBusiness()** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandler.java`
- **.handleConflict()** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandler.java`
- **GlobalExceptionHandlerAdditionalTest** (4 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandlerAdditionalTest.java`
- **.getUserDetail()** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminUserController.java`
- **.login()** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AuthController.java`
- **.toUserInfoResponse()** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AuthController.java`
- **CollectionTaskController** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CollectionTaskController.java`
- **.getStock()** (4 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/StockController.java`
- **.handleGeneric()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandler.java`
- **.handleInfrastructure()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandler.java`
- **.handleNotFound()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandler.java`
- **.handleUnauthorized()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandler.java`
- *... and 51 more nodes in this community*

## Relationships

- [[Community 32]] (4 shared connections)
- [[Community 34]] (3 shared connections)
- [[Community 37]] (2 shared connections)
- [[Community 40]] (1 shared connections)
- [[Community 118]] (1 shared connections)
- [[Collection Task Domain]] (1 shared connections)
- [[Login Log Management]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/financial/interfaces/rest/controller/FinancialAnalysisController.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandler.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AdminUserController.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/AuthController.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CollectionTaskController.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/CompanyController.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/controller/StockController.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/interfaces/rest/response/ApiResponse.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandlerAdditionalTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/advice/GlobalExceptionHandlerTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/interfaces/rest/response/ApiResponseTest.java`

## Audit Trail

- EXTRACTED: 169 (59%)
- INFERRED: 118 (41%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*