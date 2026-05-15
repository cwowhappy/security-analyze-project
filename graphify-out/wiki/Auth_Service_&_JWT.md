# Auth Service & JWT

> 18 nodes · cohesion 0.12

## Key Concepts

- **AuthAppServiceTest** (14 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.login()** (5 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/impl/AuthAppServiceImpl.java`
- **.shouldReturnLoginResultWhenLoginSuccess()** (3 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **LoginAttemptRecorder** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/LoginAttemptRecorder.java`
- **.recordFailedAttempt()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/LoginAttemptRecorder.java`
- **.recordSuccessfulLogin()** (3 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/LoginAttemptRecorder.java`
- **LoginAttemptRecorder.java** (2 connections) — `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/LoginAttemptRecorder.java`
- **.shouldThrowWhenLoginWithWrongPassword()** (2 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.setUp()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldPassWhenVerifyResetTokenValid()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldRegisterUserAndSendVerification()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldResetPasswordAndMarkTokenUsed()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldSendResetEmailWhenForgotPasswordWithExistingEmail()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldSilentlyReturnWhenForgotPasswordWithNonExistingEmail()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldThrowWhenRegisterWithDuplicateUsername()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldThrowWhenResetPasswordWithUsedToken()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldThrowWhenVerifyResetTokenExpired()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`
- **.shouldThrowWhenVerifyResetTokenNotFound()** (1 connections) — `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`

## Relationships

- [[Community 40]] (2 shared connections)
- [[Community 72]] (2 shared connections)
- [[Community 37]] (1 shared connections)
- [[Community 32]] (1 shared connections)
- [[Community 118]] (1 shared connections)

## Source Files

- `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/LoginAttemptRecorder.java`
- `backend/src/main/java/org/cwowhappy/securityanalyze/user/application/service/impl/AuthAppServiceImpl.java`
- `backend/src/test/java/org/cwowhappy/securityanalyze/user/application/service/AuthAppServiceTest.java`

## Audit Trail

- EXTRACTED: 35 (78%)
- INFERRED: 10 (22%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*