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
