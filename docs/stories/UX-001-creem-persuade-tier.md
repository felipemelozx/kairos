# Story UX-001: Creem Persuade Tier — Polish + Re-critique (Projetos + Home-logada)

**Story ID:** UX-001
**Epic:** Design System / UX Polish (transversal)
**Sprint:** 3
**Priority:** 🟡 Medium (visual polish + a11y, sem mudança de escopo)
**Points:** 3
**Effort:** 3-4 hours
**Status:** 🔄 In Progress → Done (nesta rodada)
**Type:** 🎨 UX Polish
**Owners:** @sm/@po (story) → @dev + skill impeccable (polish) → @qa (gates) → @devops (push)

---

## 📋 User Story

**Como** usuário do Kairos (visitante na landing/auth e autenticado em Projetos),
**Quero** CTAs pill pretos Persuade só em landing/auth, tipografia hero legível, cards Operate quietos com hierarquia clara e contraste AA,
**Para** entender o valor ("Only executed work counts") antes do login e operar Projetos sem fricção depois do login, com referência visual creem.io sem copiar identidade.

---

## 🎯 Objective

Fechar o shadow-work da rodada anterior (DESIGN.md tier Persuade, `.btn-persuade`, `shadow-hard`, `page.tsx` top bar + hero + mock + auth em `#signin`, `ProjectCard` hierarquia, `LoginForm`/`RegisterForm` hierarquia — tudo no working tree, não commitado) e aplicar **micro-fixes restantes sem mudar escopo**: contraste, foco, hierarquia, espaçamentos, estados.

**Alvos desta rodada (escopo fechado):**
- (a) Projetos: `src/app/projects/page.tsx` + `src/components/projects/ProjectCard.tsx` + `ProjectList.tsx` + `ProjectForm.tsx` + `ProjectDeleteDialog.tsx` + `stores/projects-store.ts`
- (b) Home-logada: estado logado de `src/app/page.tsx` (card usuário + CTA Projects + Logout)

**Fora de escopo:** novas rotas, novas features, mascotes, gamificação, dark theme, mudança de contrato API, Operate com black pill/hard shadow.

---

## 📚 Sources (No Invention)

- Crítica anterior: `.impeccable/critique/2026-09-14T11-06-26Z__apps-frontend.md` (21/40 Acceptable)
- Referência: https://www.creem.io/ (CTAs pill pretos, display pesado, cards borda grossa + hard shadow) — inspiração de densidade/hierarquia, não cópia
- `apps/frontend/DESIGN.md` — Two-Tier Rule (Persuade vs Operate), Proof-Mock Rule, One Voice Rule, contraste AA (Graphite legível, Faded Graphite só disabled)
- `docs/stories/PROJ-002-projects-ui.md` (D1–D6, error UX AUTH-002, tokens DS-001)
- Constitution Art. I (CLI First), III (Story-Driven), IV (No Invention), V (Quality First), VI (Absolute Imports `@/`)

---

## ✅ Acceptance Criteria

```gherkin
GIVEN a landing/auth em /
WHEN vejo os CTAs primários
THEN existe exatamente um .btn-persuade preto #0B0B0C pill 52-56px + borda 2px + hard shadow + seta por viewport (Start tracking, Continue with Google, Login/Register submit, Projects no card logado)
AND o secundário usa pill outline com mesma altura mínima

GIVEN o hero em /
WHEN leio o título
THEN o display usa font-display uppercase com headline de valor ("Plan honestly. Prove it with execution.") e mock estático planned-vs-executed com trilho neutro + fill verdigris + mono tabular

GIVEN cards de Projetos (/projects)
WHEN vejo Edit / Archive-Reactivate / Delete
THEN Edit é secondary 40px, Archive/Reactivate é ghost, Delete é danger-secondary com confirm (inline para Archive, dialog para Delete), todos com ícones + aria-labels e executed time mono honesto

GIVEN qualquer texto legível ou placeholder
WHEN meço contraste
THEN usa text-ink-secondary #52525B (~7.7:1 no white, AA) — nunca text-ink-muted #A1A1AA (2.56:1) em texto legível; Faded Graphite só disabled

GIVEN qualquer controle interativo
WHEN toco/clico
THEN targets >=40px (card actions, form submits, dialog, New project, Logout) e foco visível (ring verdigris ou outline UA, nunca removido sem substituto)

GIVEN a rodada
WHEN rodo os gates
THEN cd apps/frontend && npm run lint && npm run typecheck && npm test passam (e build se necessário), sem regressão de suites

GIVEN o re-critique
WHEN comparo com creem.io nos dois alvos
THEN novo snapshot em .impeccable/critique/ com notas por heurística + severidade P0-P3 e score atual vs 21/40
```

---

## ✅ Tasks

### Phase 1 — Story (@sm/@po)
- [x] 1.1 Criar `docs/stories/UX-001-creem-persuade-tier.md` (este arquivo) com AC, checklist e file list
- [x] 1.2 Amarrar shadow-work anterior a esta story (DESIGN.md, globals.css, tailwind, page.tsx, auth forms, projects/*)

### Phase 2 — Polish (@dev + skill impeccable)
- [x] 2.1 `globals.css`: `.btn-persuade` com `:focus-visible` + `:disabled` (sem remover outline sem substituto)
- [x] 2.2 `ProjectForm.tsx`: placeholder `text-ink-secondary`, presets 40px, botões `min-h-[40px]`, color well com focus ring
- [x] 2.3 `ProjectList.tsx` + `ProjectCard.tsx` (confirm) + `ProjectDeleteDialog.tsx`: botões primários/secundários com `min-h-[40px]` + `inline-flex`
- [x] 2.4 `page.tsx` home-logada: card com borda, Logout `min-h-[40px]`, CTA Projects Persuade w-full
- [x] 2.5 Sem escopo novo: Operate nunca usa black pill/hard shadow; Persuade só landing/auth

### Phase 3 — Re-critique (skill impeccable)
- [x] 3.1 Re-critique dedicada aos dois alvos (projetos + home-logada) vs creem.io (botões/tipografia/cards)
- [x] 3.2 Persistir snapshot em `.impeccable/critique/<timestamp>__ux-001-projects-homelogada.md` com score, heurísticas, P0-P3

### Phase 4 — Gates (@qa)
- [x] 4.1 `cd apps/frontend && npm run lint && npm run typecheck && npm test` (e `npm run build` se necessário)
- [x] 4.2 Relatar suites/testes e falhas com correção

### Phase 5 — Push (@devops, só porque o usuário pediu)
- [ ] 5.1 Inspecionar `git status/diff/log`, selecionar só intencionais (frontend + DESIGN.md + story UX + testes; conferir backend PROJ-001/PROJ-002 — separar se ambíguo, nunca segredo)
- [ ] 5.2 Commit Conventional Commits conciso + push; retornar URL/estado

---

## 📝 Dev Agent Record (engine-mode — delegate bloqueado depth 1)

| Timestamp | Phase | Action | Result |
|-----------|-------|--------|--------|
| 2026-09-14 | 1 | Story UX-001 criada em `docs/stories/` | Este arquivo |
| 2026-09-14 | 2 | Micro-fixes polish (ver File List) | 6 arquivos: globals.css (:focus-visible/:disabled), ProjectForm (placeholder AA + presets 40px + min-h + color-well ring), ProjectList/ProjectCard-confirm/DeleteDialog (min-h 40px inline-flex), page.tsx (card border + Logout 40px) |
| 2026-09-14 | 3 | Re-critique projetos + home-logada | `.impeccable/critique/2026-09-14T12-30-00Z__ux-001-projects-homelogada.md` — 29/40 Good (baseline 21/40, +8, P1 zerados; gitignored, não commitado) |
| 2026-09-14 | 4 | Gates lint/typecheck/test/build | lint 0, typecheck 0, test 19 suites/226 passed, build ok (/, /projects, /_not-found) |
| 2026-09-14 | 5 | Push @devops | A registrar |

---

## 📁 File List

**Shadow-work anterior (amarrado a esta story):**
- `apps/frontend/DESIGN.md` (MOD — tier Persuade)
- `apps/frontend/src/app/globals.css` (MOD — `.btn-persuade` + `.placeholder-readable`)
- `apps/frontend/tailwind.config.ts` (MOD — `shadow-hard`, `hard-lg`)
- `apps/frontend/src/app/page.tsx` (MOD — top bar + hero + mock + auth `#signin`)
- `apps/frontend/src/app/page.test.tsx` (MOD)
- `apps/frontend/src/components/auth/LoginForm.tsx` (MOD)
- `apps/frontend/src/components/auth/RegisterForm.tsx` (MOD)
- `apps/frontend/src/components/projects/*`, `src/app/projects/*`, `src/lib/projects-*`, `src/stores/projects-store*` (NEW — PROJ-002, re-polidos aqui)
- `docs/stories/AUTH-001-login.md`, `DS-001-design-tokens.md`, `SEC-001-csrf-protection.md` (MOD — checklist)
- Backend PROJ-001/PROJ-002 (`ProjectApi.java`, `ProjectController.java`, DTOs, `Project.java`, `ProjectStatus.java`, `ProjectRepository.java`, `ProjectService.java`, `V2__Create_projects.sql`, testes) — **conferir no push se entram ou ficam de fora**

**Esta rodada:**
- `docs/stories/UX-001-creem-persuade-tier.md` (NEW — este arquivo)
- `.impeccable/critique/<timestamp>__ux-001-projects-homelogada.md` (NEW — re-critique)
- Polish edits: `globals.css`, `ProjectForm.tsx`, `ProjectList.tsx`, `ProjectCard.tsx`, `ProjectDeleteDialog.tsx`, `page.tsx` (MOD — ver Dev Notes)

---

## 🔗 Dependencies

- Bloqueado por: PROJ-002 (Done), DS-001 (Done), AUTH-001/AUTH-002 (Done)
- Bloqueia: nada (polish transversal, não bloqueia TB-001/SESSION-001)

---

## ⚠️ Risks

| Risk | Mitigation |
|------|------------|
| Misturar tiers (black pill no Operate) | Two-Tier Rule: Operate verdigris flat; Persuade black pill só `/` + auth |
| Quebrar testes PROJ-002 | Rodar gates antes do push; classes são aditivas |
| Commitar backend junto sem querer | @devops inspeciona diff; separa se ambíguo |

---

## 📋 Definition of Done

- [x] Story criada com AC + checklist + file list; shadow-work amarrado
- [x] Polish aplicado sem mudança de escopo
- [x] Re-critique persistida com score vs 21/40 (29/40 Good, P1 zerados; .impeccable é gitignored)
- [x] Gates verdes (frontend: lint/typecheck/test 19/226/build; backend: 87 testes BUILD SUCCESS)
- [ ] Push feito só com arquivos intencionais (sem segredo) — em execução @devops
- [ ] Temporários limpos (detect_*.json/txt no root)
