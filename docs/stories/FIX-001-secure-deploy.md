# Story FIX-001: Secure Deploy + Health

**Epic:** Hardening MVP
**Story ID:** FIX-001
**Priority:** Critical
**Status:** Done
**Type:** Fix

## Objective
Bloquear deploy inseguro: perfil prod, secrets obrigatórios, compose sem portas públicas, health check funcional.

## Acceptance Criteria
```gherkin
GIVEN vars JWT_SECRET/CSRF_SECRET ausentes ou placeholder
WHEN backend inicia em prod
THEN falha fast com mensagem clara

GIVEN deploy prod
THEN postgres/redis não publicam portas no host
AND /api/actuator/health responde 200
AND deploy depende de CI verde
```

## Tasks
- [x] application-prod.properties + validacao secrets no startup
- [x] Actuator + management port/path alinhado (Dockerfile, nginx, workflow, docs)
- [x] docker-compose prod seguro (sem ports db/redis, creds via env)
- [x] deploy-backend depende de CI / usa artefato validado
- [x] docs DEPLOYMENT atualizados

## File List

- [x] .github/workflows/deploy-backend.yml
- [x] apps/backend/pom.xml
- [x] apps/backend/src/main/java/com/felipemelozx/kairos/config/RequiredSecretsValidator.java (NEW)
- [x] apps/backend/src/main/resources/application-prod.properties (NEW)
- [x] apps/backend/src/main/resources/application.yml
- [x] docker-compose.yml
- [x] docker-compose.override.yml (NEW)
- [x] infrastructure/docker/Dockerfile.backend
- [x] infrastructure/vps/nginx.conf
- [x] DEPLOYMENT.md

## Validation
- [x] Backend 165/165 verdes; Actuator adicionado; health /api/actuator/health alinhado em Dockerfile/nginx/workflow/docs
