# Backend Financial Repository Tests

> 18 nodes · cohesion 0.12

## Key Concepts

- **main.ts** (6 connections) — `frontend/src/main.ts`
- **index.ts** (6 connections) — `frontend/src/router/index.ts`
- **auth.ts** (6 connections) — `frontend/src/stores/modules/auth.ts`
- **auth.spec.ts** (4 connections) — `frontend/src/stores/modules/__tests__/auth.spec.ts`
- **useAuthStore** (3 connections) — `frontend/src/stores/modules/auth.ts`
- **App.vue** (1 connections) — `frontend/src/App.vue`
- **LoginCredentials** (1 connections) — `frontend/src/stores/modules/auth.ts`
- **RegisterData** (1 connections) — `frontend/src/stores/modules/auth.ts`
- **UserInfo** (1 connections) — `frontend/src/stores/modules/auth.ts`
- **authStore** (1 connections) — `frontend/src/router/index.ts`
- **isPublic** (1 connections) — `frontend/src/router/index.ts`
- **requiresAdmin** (1 connections) — `frontend/src/router/index.ts`
- **router** (1 connections) — `frontend/src/router/index.ts`
- **routes** (1 connections) — `frontend/src/router/index.ts`
- **app** (1 connections) — `frontend/src/main.ts`
- **authStore** (1 connections) — `frontend/src/main.ts`
- **createMockStorage()** (1 connections) — `frontend/src/stores/modules/__tests__/auth.spec.ts`
- **store** (1 connections) — `frontend/src/stores/modules/__tests__/auth.spec.ts`

## Relationships

- No strong cross-community connections detected

## Source Files

- `frontend/src/App.vue`
- `frontend/src/main.ts`
- `frontend/src/router/index.ts`
- `frontend/src/stores/modules/__tests__/auth.spec.ts`
- `frontend/src/stores/modules/auth.ts`

## Audit Trail

- EXTRACTED: 38 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [[index]] to navigate.*