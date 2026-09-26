# Story FIX-003: Frontend Auth + Contracts

**Epic:** Hardening MVP
**Story ID:** FIX-003
**Priority:** High
**Status:** Done
**Type:** Fix

## Objective
Corrigir bootstrap auth, refresh single-flight, isolamento de stores, senha em memória, validação edit, foco/contraste, csrf decode, contratos monorepo/Bruno.

## Acceptance Criteria
```gherkin
GIVEN cookies válidos e acesso direto a /projects
WHEN página monta
THEN não redireciona antes de /auth/me concluir

GIVEN 2+ requests 401 simultâneos
WHEN tokens expirados
THEN apenas 1 POST /api/auth/refresh

GIVEN logout ou troca de usuário
WHEN nova sessão inicia
THEN projetos antigos nunca renderizam

GIVEN login -> logout na mesma SPA
WHEN volta ao LoginForm
THEN campo senha vazio
```

## Tasks
- [x] Auth status checking/authenticated/anonymous + replace redirect
- [x] apiFetch single-flight refresh + limpeza sessão 401 terminal
- [x] projectsStore reset por usuário + não render lista com erro
- [x] Remover senha do draft / limpar no login
- [x] requireColor apenas create
- [x] csrf decode seguro, break-words, foco dialogs, contraste
- [x] shared real ou removido, turbo/CI, Bruno, docs

## File List

- [x] apps/frontend/src/stores/auth-store.ts
- [x] apps/frontend/src/components/ProtectedRoute.tsx
- [x] apps/frontend/src/lib/api.ts
- [x] apps/frontend/src/stores/projects-store.ts
- [x] apps/frontend/src/components/projects/ProjectList.tsx
- [x] apps/frontend/src/app/page.tsx
- [x] apps/frontend/src/components/projects/ProjectForm.tsx
- [x] apps/frontend/src/lib/csrf.ts
- [x] apps/frontend/src/components/projects/ProjectCard.tsx
- [x] apps/frontend/src/lib/api.test.ts
- [x] apps/frontend/src/lib/csrf.test.ts
- [x] apps/frontend/src/components/ProtectedRoute.test.tsx
- [x] apps/frontend/src/app/projects/page.test.tsx
- [x] apps/frontend/src/stores/projects-store.test.ts
- [x] apps/frontend/next.config.js
- [x] apps/frontend/vercel.json
- [x] packages/shared/src/index.ts (NEW)
- [x] packages/shared/tsconfig.json (NEW)
- [x] packages/shared/package.json
- [x] .github/workflows/ci.yml
- [x] apps/backend/bruno/auth/register.bru
- [x] apps/backend/bruno/environments/dev.bru
- [x] apps/backend/bruno/time-blocks/create.bru

## Validation
- [x] Frontend 237/237 verdes + lint + typecheck + next build OK; shared tsc OK
