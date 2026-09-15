# Story UI-001: Neo-Brutalism Portfolio Style (Peach + White, sem preto)

**Story ID:** UI-001
**Epic:** Design System / UX Polish
**Priority:** High
**Status:** Done
**Type:** 🎨 UI Restyle
**Scope:** `apps/frontend` apenas

## User Story

Como usuário do Kairos,
Quero a aplicação com o estilo neo-brutalista do portfólio (`/home/felipemelozx/projects/portifolio`),
Para ter identidade visual consistente (bordas grossas, sombras sólidas, cards destacados),
mas com botões Peach + Branco (nunca pretos).

## Sources (No Invention)

- Referência: `/home/felipemelozx/projects/portifolio/DESIGN.md` (violet `#C4B5DE`, peach `#FFD4B8`, green `#4ADE80`, dark `#111111`, light `#FAF8F5`, Inter + IBM Plex Mono, border-2, solid shadows, hover translate)
- Referência: `/home/felipemelozx/projects/portifolio/src/app/globals.css` (`.nb-card`, `.feature-card`, `.status-badge`, `.project-link-button.primary`)
- Referência: `/home/felipemelozx/projects/portifolio/tailwind.config.ts` (`brand.*`, `shadow nb-sm/md/lg/xl`)
- Decisão usuário (2026-09-14): botões Peach + Branco, escopo só frontend.

## Acceptance Criteria

- [x] Nenhum botão primario usa fill preto (`#0B0B0C` / `bg-dark-block` / `.btn-persuade` preto removido ou recolorido para peach)
- [x] Botão primário = peach `#FFD4B8` + texto dark + `border-2` + `shadow-nb-md` + hover translate; secundário = branco mesma borda/sombra
- [x] Cards/containers = `bg-white` (ou violet/peach em destaques) + `border-2 brand-dark` + `rounded-xl/2xl` + `shadow-nb-md` + hover `translate + shadow-lg`
- [x] Tokens NB adicionados de forma aditiva (não quebra `design-tokens.test.ts`: `--color-accent` e hexes legados permanecem)
- [x] Gates: `npm run lint && npm run typecheck && npm test` verdes (lint 0, typecheck 0, 19 suites/226 testes, build ok)

## Tasks

- [x] Criar esta story
- [x] `tailwind.config.ts`: adicionar `brand.*`, `shadow nb-*`, fonts Inter (aditivo)
- [x] `globals.css`: adicionar tokens brand + `.btn-nb-primary/.btn-nb-secondary/.nb-card*/.status-badge/.input-nb`, recolorir `.btn-persuade` para peach (sem preto)
- [x] `layout.tsx`: Inter + IBM Plex Mono
- [x] `page.tsx`: navbar flutuante NB, CTAs peach/branco, preview e auth em `nb-card`
- [x] `ProjectCard/List/Form/DeleteDialog` + `LoginForm/RegisterForm`: botões peach/branco, cards NB, inputs `border-2`
- [x] Rodar gates e limpar resíduos (`btn-persuade` preto, `bg-dark-block`, `shadow-sm` em cards)

## File List

- `docs/stories/UI-001-neo-brutalism-portfolio.md` (NEW — este arquivo)
- `apps/frontend/tailwind.config.ts` (MOD)
- `apps/frontend/src/app/globals.css` (MOD)
- `apps/frontend/src/app/layout.tsx` (MOD)
- `apps/frontend/src/app/page.tsx` (MOD)
- `apps/frontend/src/components/projects/ProjectCard.tsx` (MOD)
- `apps/frontend/src/components/projects/ProjectList.tsx` (MOD)
- `apps/frontend/src/components/projects/ProjectForm.tsx` (MOD)
- `apps/frontend/src/components/projects/ProjectDeleteDialog.tsx` (MOD)
- `apps/frontend/src/components/auth/LoginForm.tsx` (MOD)
- `apps/frontend/src/components/auth/RegisterForm.tsx` (MOD)

## Notes

- ~~`apps/frontend/DESIGN.md` frontmatter NÃO atualizado nesta story~~ — **follow-up concluído em 2026-09-14:** frontmatter atualizado (cores `brand-*`, canvas `#FAF8F5`, ink `#111111`, tipografia Inter/IBM Plex Mono, componentes peach/NB). Teste de paridade passa (63 asserts, +4 novos). Tokens legados mantidos para compatibilidade.
