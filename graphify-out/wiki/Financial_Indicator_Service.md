# Financial Indicator Service

> 26 nodes · cohesion 0.10

## Key Concepts

- **JdbcFinancialIndicatorRepository** (9 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **FinancialIndicatorRepository** (7 connections)
- **JdbcFinancialIndicatorRepositoryTest** (7 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`
- **.buildIndicator()** (5 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`
- **.save()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **FinancialIndicatorAppService** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppService.java`
- **FinancialIndicatorAppServiceTest** (3 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppServiceTest.java`
- **FinancialIndicatorAppService.java** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppService.java`
- **JdbcFinancialIndicatorRepository.java** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **FinancialIndicatorAppServiceTest.java** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppServiceTest.java`
- **JdbcFinancialIndicatorRepositoryTest.java** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`
- **.saveAll()** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **.shouldFindLatestByReportType()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`
- **.shouldSaveAllBatch()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`
- **.shouldSaveAndFindIndicatorByStockCode()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`
- **.shouldUpsertWhenSaveExistingRecord()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`
- **.findByStockCode()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **.findByStockCodeAndReportDate()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **.findLatest()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **.findLatestByStockCodes()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **.toDomain()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- **.configureProperties()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`
- **.getIndicators()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppService.java`
- **.toDTO()** (1 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppService.java`
- **.shouldReturnEmptyListWhenNoIndicators()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppServiceTest.java`
- *... and 1 more nodes in this community*

## Relationships

- [[Community 138]] (1 shared connections)
- [[Community 43]] (1 shared connections)
- [[Company Domain & Repository]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppService.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepository.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/financial/application/service/FinancialIndicatorAppServiceTest.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/financial/infrastructure/persistence/repository/JdbcFinancialIndicatorRepositoryTest.java`

## Audit Trail

- EXTRACTED: 64 (98%)
- INFERRED: 1 (2%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*