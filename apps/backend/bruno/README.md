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

O Bruno gerencia cookies automaticamente. Após login/register, os cookies httpOnly são enviados automaticamente nas próximas requests.

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

## Documentação Completa

Veja `docs/stories/AUTH-001-login.md` para especificação completa dos endpoints.
