# Kairos Monorepo

Sistema de produtividade centrado em tempo — monorepo com React (Next.js) no frontend e Java Spring Boot no backend.

---

## 📁 Estrutura

```
kairos/
├── apps/
│   ├── backend/              # Spring Boot API (Java 21)
│   │   ├── .mvn/
│   │   ├── mvnw / mvnw.cmd
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/
│   │       │   └── resources/
│   │       │       ├── application.properties        # ativa perfil dev
│   │       │       └── application-dev.properties    # config local
│   │       └── test/
│   └── frontend/             # Next.js 14 (React + TypeScript)
│       ├── src/
│       │   ├── app/
│       │   ├── components/
│       │   ├── lib/
│       │   └── types/
│       └── package.json
├── packages/
│   └── shared/               # Tipos e utilitários compartilhados
│       └── package.json
├── infrastructure/
│   ├── docker/               # Dockerfiles
│   ├── nginx/                # Config Nginx
│   └── vps/                  # Scripts de deploy
├── docs/                     # Documentação única
│   ├── prd.md
│   ├── BACKEND_GUIDELINES.md
│   └── diagrams/
├── .github/
│   └── workflows/            # CI/CD pipelines
├── docker-compose.yml        # Infra local (Postgres + Redis)
├── package.json              # Root workspace (Turbo)
└── turbo.json                # TurboRepo v2 config
```

---

## 🚀 Quick Start

### Pré-requisitos

- **Node.js** 20+
- **Java** 21
- **Docker & Docker Compose**

### Desenvolvimento local

**1. Instalar dependências JS:**
```bash
npm install
```

**2. Subir infraestrutura (Postgres + Redis):**
```bash
npm run docker:up
```

**3. Rodar backend (terminal 1):**
```bash
npm run backend:dev
```

**4. Rodar frontend (terminal 2):**
```bash
npm run frontend:dev
```

---

## 📦 Scripts disponíveis

### Raiz (TurboRepo)
| Script | Descrição |
|---|---|
| `npm run dev` | Roda todos os apps em modo dev |
| `npm run build` | Build de todos os apps |
| `npm run test` | Roda todos os testes |
| `npm run lint` | Lint em todos os apps |
| `npm run clean` | Remove todos os artefatos de build |

### Backend
| Script | Descrição |
|---|---|
| `npm run backend:dev` | Inicia Spring Boot com hot reload |
| `npm run backend:build` | Gera o JAR de produção |

### Frontend
| Script | Descrição |
|---|---|
| `npm run frontend:dev` | Inicia Next.js em dev (localhost:3000) |
| `npm run frontend:build` | Build de produção |

### Docker
| Script | Descrição |
|---|---|
| `npm run docker:up` | Sobe Postgres e Redis |
| `npm run docker:down` | Para os containers |
| `npm run docker:logs` | Segue os logs |

---

## 🔧 Configuração

### Backend

O perfil `dev` é ativado automaticamente via `application.properties`.

Edite `apps/backend/src/main/resources/application-dev.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/kairos_db
spring.datasource.username=kairos
spring.datasource.password=kairos
spring.redis.host=localhost
spring.redis.port=6379
```

### Frontend

Crie `apps/frontend/.env.local`:
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

---

## 🌐 Deploy

### Frontend → Vercel

```bash
cd apps/frontend
vercel --prod
```

Ou push para `main` (CI/CD automático via GitHub Actions).

### Backend → VPS

```bash
./infrastructure/vps/deploy-backend.sh
```

Ou push para `main` (CI/CD automático via GitHub Actions).

---

## 🔐 GitHub Secrets necessários

| Secret | Descrição |
|---|---|
| `VPS_HOST` | IP ou domínio do VPS |
| `VPS_USER` | Usuário SSH |
| `VPS_SSH_PRIVATE_KEY` | Chave SSH privada |
| `VERCEL_TOKEN` | Token da API do Vercel |
| `VERCEL_ORG_ID` | ID da organização no Vercel |
| `VERCEL_PROJECT_ID` | ID do projeto no Vercel |

---

## 📚 Documentação

- [PRD — Product Requirements](docs/prd.md)
- [Backend Guidelines](docs/BACKEND_GUIDELINES.md)
- [Data Model](docs/diagrams/)

---

## 🔍 Troubleshooting

### Backend não inicia

1. Verifique se o Postgres está rodando: `docker ps`
2. Verifique os logs: `npm run docker:logs`
3. Confirme que o perfil ativo é `dev` no log de boot

### Frontend não encontra o backend

1. Backend rodando: `curl http://localhost:8080/actuator/health`
2. `NEXT_PUBLIC_API_URL` definido em `apps/frontend/.env.local`
3. Configuração CORS no backend

### Falha no deploy VPS

1. Acesso SSH: `ssh $VPS_USER@$VPS_HOST`
2. Docker instalado no VPS
3. Portas 80, 443 abertas no firewall
