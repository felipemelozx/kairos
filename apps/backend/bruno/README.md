# Kairos API - Bruno Collection

Collection do [Bruno](https://www.usebruno.com/) para testar a API Kairos.

## Instalação

1. Baixe o Bruno: https://www.usebruno.com/
2. Clone este repositório
3. Abra o Bruno e selecione "Open Collection"
4. Navegue até `apps/backend/bruno/` e selecione a pasta

## Environments

A collection possui 3 environments configurados:

- **dev**: `http://localhost:8080` (desenvolvimento local)
- **staging**: `https://staging.kairos.io` (ambiente de staging)
- **prod**: `https://api.kairos.io` (produção)

Para trocar de environment, clique no dropdown no canto superior direito do Bruno.

## Endpoints Disponíveis

### Auth

- **Register** - `POST /api/auth/register`
  - Cria novo usuário com email/password
  - Retorna cookies httpOnly (ACCESS_TOKEN, REFRESH_TOKEN)
  
- **Login** - `POST /api/auth/login`
  - Autentica com email/password
  - Retorna cookies httpOnly
  
- **Logout** - `POST /api/auth/logout`
  - Limpa cookies de autenticação
  
- **Refresh Token** - `POST /api/auth/refresh`
  - Renova ACCESS_TOKEN usando REFRESH_TOKEN cookie
  
- **Get Current User** - `GET /api/auth/me`
  - Retorna perfil do usuário autenticado
  - Requer ACCESS_TOKEN cookie válido

### Projects

> Requer ACCESS_TOKEN cookie (login/register primeiro). Requests de escrita
> (`POST`/`PATCH`/`DELETE`) exigem também o header `X-CSRF-Token` — a variável
> de ambiente `csrfToken` é preenchida automaticamente pelo script de
> pós-resposta do login/register/refresh. Nunca cole o valor manualmente.

- **Create Project** - `POST /api/projects` → 201
  - Body: `name` (1–100), `description` (max 500, opcional), `color` (#RRGGBB)
  - 400 se validação falhar
- **List Projects** - `GET /api/projects` → 200
- **Get Project** - `GET /api/projects/:projectId` → 200/404
- **Update Project** - `PATCH /api/projects/:projectId` → 200 (partial update)
- **Delete Project** - `DELETE /api/projects/:projectId` → 200 (soft delete)

IDs de outro usuário retornam 404 (owner scoping, nunca 403).

### Time Block Series

> Requer ACCESS_TOKEN cookie (login/register primeiro). Requests de escrita
> (`POST`/`PATCH`/`DELETE`) exigem também o header `X-CSRF-Token` — a variável
> de ambiente `csrfToken` é preenchida automaticamente pelo script de
> pós-resposta do login/register/refresh. Nunca cole o valor manualmente.

- **Create Series** - `POST /api/time-block-series` → 201
  - Body: `title` (1–200), `projectId` opcional, `startDate`, `endDate` opcional (>= startDate),
    `pattern`: `type` DAILY | DAILY_INTERVAL | WEEKLY_WEEKDAYS | SINGLE, `interval` (>=1, DAILY_INTERVAL),
    `weekdays` (subset não-vazio de 0-6, 0=domingo, WEEKLY_WEEKDAYS), `time` HH:mm, `durationMinutes` 1–1440
  - Materializa occurrences em `time_blocks` até min(endDate, hoje+12 meses), cap 366
  - 400 INVALID_SERIES_PATTERN / INVALID_SERIES_RANGE, 404 se `projectId` não pertence ao usuário
- **List Series** - `GET /api/time-block-series` → 200
- **Get Series** - `GET /api/time-block-series/:seriesId` → 200/404
- **Update Series** - `PATCH /api/time-block-series/:seriesId` → 200 (split forward)
  - Body: `effectiveFrom` (obrigatório, após o startDate da série) + campos de regra (title/projectId/endDate/pattern)
  - A série antiga fecha em effectiveFrom-1; nova série + occurrences a partir de effectiveFrom; passado intacto
  - 400 INVALID_SERIES_RANGE se effectiveFrom <= startDate
- **Delete Series** - `DELETE /api/time-block-series/:seriesId` → 200 (soft delete série + occurrences futuras; passado preservado)

IDs de outro usuário retornam 404 (owner scoping, nunca 403).

### Time Blocks

> Requer ACCESS_TOKEN cookie (login/register primeiro). Requests de escrita
> (`POST`/`PATCH`/`DELETE`) exigem também o header `X-CSRF-Token` — a variável
> de ambiente `csrfToken` é preenchida automaticamente pelo script de
> pós-resposta do login/register/refresh. Nunca cole o valor manualmente:
> o token CSRF é rotacionado a cada autenticação e a colagem invalida.

- **Create Time Block** - `POST /api/time-blocks` → 201
  - Body: `title` (1–200), `startDateTime`, `endDateTime` (ISO-8601), `projectId` opcional
  - 400 se título/range inválido, 404 se `projectId` não pertence ao usuário
- **List Time Blocks** - `GET /api/time-blocks` → 200
- **List By Range** - `GET /api/time-blocks?from=...&to=...` → 200
  - Ambos juntos, `to` após `from`, senão 400
- **Get Time Block** - `GET /api/time-blocks/:blockId` → 200/404
- **Update Time Block** - `PATCH /api/time-blocks/:blockId` → 200 (partial update)
- **Delete Time Block** - `DELETE /api/time-blocks/:blockId` → 200 (soft delete)

IDs de outro usuário retornam 404 (owner scoping, nunca 403).

## Como Usar

### 1. Registrar usuário

```json
{
  "email": "test@example.com",
  "password": "password123",
  "name": "Test User"
}
```

### 2. Login

Após registrar, use o mesmo email/password para login.

### 3. Testar endpoints autenticados

O Bruno gerencia cookies automaticamente. Após login/register, os cookies httpOnly são enviados automaticamente nas próximas requests, e o script de pós-resposta captura o cookie `CSRF_TOKEN` para a var `csrfToken` (usada pelo header `X-CSRF-Token`).

### 4. Verificar autenticação

Use `GET /api/auth/me` para verificar se está autenticado.

## Cookies httpOnly

A API usa cookies httpOnly para segurança (não localStorage). O Bruno suporta isso nativamente - após login, os cookies são automaticamente incluídos nas requests subsequentes.

## Troubleshooting

### 401 Unauthorized em /api/auth/me

- Verifique se fez login/register antes
- Confirme que o environment está correto (dev/staging/prod)
- Verifique se o backend está rodando em `http://localhost:8080`

### Backend não responde

- Inicie o backend: `cd apps/backend && ./mvnw spring-boot:run`
- Aguarde até ver "Started KairosApplication"
- Verifique se a porta 8080 está livre

### WARN "Request without Origin and Referer headers" no log

- É só um aviso: sem `Origin`/`Referer` (caso do Bruno) o filtro libera a request (fail-open). **Não bloqueia.**
- O erro real está no status HTTP da resposta: `401` = faça login primeiro; `403` = `X-CSRF-Token` ausente/errado (rode login/register/refresh de novo para o script recapturar o `csrfToken` — nunca cole manualmente); `403 "Invalid origin"` = você enviou um header `Origin` fora da allowlist — remova o header ou use `http://localhost:3000`.

## Documentação Completa

Veja `docs/stories/AUTH-001-login.md` para especificação completa dos endpoints.
