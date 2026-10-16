# AIOX-001: Remover o AIOX Core do repositório

## Story

**Como** responsável pelo projeto Kairos,
**quero** remover o AIOX Core e suas integrações do repositório,
**para** manter o projeto sem dependência do framework AIOX, preservando as funcionalidades do produto.

## Metadados

| Campo | Valor |
|---|---|
| Prioridade | Alta |
| Status | Ready for Review |
| Executor | @dev |
| Origem | Solicitação explícita do usuário |

## Escopo

### Incluído

- Remover integralmente o diretório `.aiox-core/`.
- Remover ou adaptar referências, configurações, scripts e documentação do repositório que pressuponham o AIOX Core, incluindo `AGENTS.md`, `opencode.json`, `.coderabbit.yaml` e workflows do GitHub quando aplicável.
- Remover comandos e instruções de instalação, validação ou sincronização do AIOX que deixem de funcionar após a remoção.
- Preservar os aplicativos, pacotes e funcionalidades de produto que não dependam do AIOX.
- Atualizar esta story com checklist e lista final de arquivos alterados/removidos.

### Fora do escopo

- Alterações funcionais no produto sem relação necessária com a remoção do framework.
- Remoção de menções a AIOX em conteúdo de terceiros ou histórico que não configure integração ativa; justificar qualquer referência residual relevante.

## Critérios de Aceitação

- [x] `.aiox-core/` e seus arquivos rastreados foram removidos do repositório.
- [x] Configurações, workflows de CI/deploy, regras do CodeRabbit e arquivos de container não dependem de arquivos AIOX removidos.
- [x] Não há comandos AIOX nos scripts do monorepo; instruções AIOX foram retiradas do arquivo local `AGENTS.md` (ignorado pelo Git).
- [x] A busca por referências operacionais a AIOX fora desta story não encontrou resultados.
- [x] Build do frontend e empacotamento do backend passam sem o AIOX Core; testes do frontend passam em execução serial.
- [x] Checklist e File List desta story estão atualizados.

## Tarefas / Subtarefas

- [x] Inventariar referências e dependências de AIOX fora de `.aiox-core/`.
- [x] Remover/adaptar referências operacionais em configuração, CI, deploy e arquivos de container.
- [x] Remover `.aiox-core/` e seus arquivos rastreados.
- [x] Executar busca de referências residuais e revisar os resultados.
- [x] Executar os quality gates disponíveis e registrar os bloqueios ambientais/de configuração.
- [x] Atualizar este checklist, File List e status da story com os resultados.

## Notas de Implementação

- A branch de trabalho solicitada é `chore/remove-aiox-core`.
- A remoção deve preservar o código e comportamento dos produtos Kairos; atualizar somente os arquivos de integração/documentação quando necessário para remover dependência do AIOX.
- Não foi localizado épico ou PRD relacionado em `docs/prd/`; esta story foi aberta diretamente a partir da solicitação explícita do usuário.
- Verificação: frontend lint/typecheck/build passaram e os 234 testes passaram com `npm test -- --runInBand`. A execução paralela de `npm test` excedeu tempos de teste existentes.
- Verificação: `./mvnw test` não completou porque Testcontainers não encontrou Docker (`/var/run/docker.sock` ausente); `./mvnw -DskipTests package` passou.
- Verificação: `packages/shared` não define `test`; `lint` falha porque não há `src/` e `typecheck` não encontra projeto TypeScript/tsconfig para compilar.
- Observação: `.aiox-core/node_modules` era uma instalação local ignorada pelo Git; foi removida junto com o diretório após confirmar que não continha arquivos rastreados.

## File List

- `.aiox-core/` — removidos 1.191 arquivos rastreados e dependências locais ignoradas.
- `AGENTS.md` — removidas as instruções AIOX da cópia local, ignorada pelo Git.
- `.coderabbit.yaml` — removido filtro de exclusão AIOX e referências à constituição do framework.
- `.github/workflows/deploy-backend.yml` — removida exclusão de diretório AIOS obsoleta.
- `.github/workflows/trivy.yml` — removida exclusão do diretório AIOX.
- `.gitignore` — removidas regras/comentários de arquivos locais AIOS/AIOX.
- `infrastructure/docker/.dockerignore` — removida exclusão obsoleta AIOS.
- `opencode.json` — removida instrução para carregar a constituição AIOX.
- `docs/stories/AIOX-001-remove-aiox-core.md` — checklist, resultados e File List atualizados.

## Histórico

| Versão | Data | Alteração |
|---|---|---|
| 1.0 | 2026-10-16 | Story criada a partir da solicitação do usuário. |
