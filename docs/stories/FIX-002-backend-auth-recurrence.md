# Story FIX-002: Backend Auth + Recorrência

**Epic:** Hardening MVP
**Story ID:** FIX-002
**Priority:** Critical
**Status:** Done
**Type:** Fix

## Objective
Fechar isolamento cross-user, sessão de inativos, recorrência truncada/DoS, erros 400, concorrência e migrations.

## Acceptance Criteria
```gherkin
GIVEN series de user A
WHEN user B POST /api/time-blocks {seriesId: A}
THEN 404 e nada criado

GIVEN user desativado com access/refresh válidos
WHEN chama API ou refresh
THEN 401

GIVEN serie diária iniciada há >366 dias
WHEN roll-forward roda
THEN ocorrências próximas ao horizonte existem

GIVEN DAILY_INTERVAL extremo ou startDate extremo
WHEN cria série
THEN 400 ou completa em tempo limitado

GIVEN UUID/enum/JSON inválidos
WHEN chama API
THEN 400 com envelope ApiResponse, nunca 500
```

## Tasks
- [x] Validar seriesId owner-scoped em TimeBlockService
- [x] Checar active em filter + refresh + me
- [x] Roll-forward incremental + incluir séries finitas
- [x] RecurrenceRule com limites e salto por intervalo
- [x] GlobalExceptionHandler 400s + DataIntegrity 409
- [x] @Version nas entidades + lock split
- [x] V7 reconciliação series_id + ddl validate em prod/test
- [x] Testes de regressão

## File List

- [x] apps/backend/src/main/java/com/felipemelozx/kairos/service/TimeBlockService.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/service/AuthService.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/service/RecurrenceRule.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/service/SeriesRollForwardService.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/security/jwt/JwtCookieAuthenticationFilter.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/repository/UserRepository.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/repository/TimeBlockSeriesRepository.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/entity/TimeBlock.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/entity/Project.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/entity/TimeBlockSeries.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/dto/request/RegisterRequest.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/dto/request/LoginRequest.java
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/exception/GlobalExceptionHandler.java
- [x] apps/backend/src/main/resources/db/migration/V7__Reconcile_series_and_locking.sql (NEW)
- [x] apps/backend/src/test/java/com/felipemelozx/kairos/service/TimeBlockServiceTest.java
- [x] apps/backend/src/test/java/com/felipemelozx/kairos/service/SeriesRollForwardServiceTest.java

## Validation
- [x] Backend 165/165 verdes (incl. novos testes seriesId cross-user, ocorrência de série, roll-forward incremental)
