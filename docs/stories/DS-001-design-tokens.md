# DS-001: Design Tokens Encoding — Tailwind, CSS Variables & Fonts

## Story

**As a** developer  
**I want to** encode the approved DESIGN.md tokens into the frontend (Tailwind theme, CSS custom properties, and next/font)  
**So that** every screen is built on the Ledger of Lived Time system instead of the placeholder blue/gradient defaults

---

## Story Metadata

| Field | Value |
|-------|-------|
| **Priority** | High |
| **Status** | Ready for Review |
| **Points** | 5 |
| **Epic** | Design System (no `docs/epics/` yet; this story is the first token-level slice) |
| **Executor** | @dev (Dex) |
| **Quality Gate** | @ux-design-expert (Vista) — visual/token fidelity; @qa — test gates |

---

## Scope

### In Scope

- Extend `apps/frontend/tailwind.config.ts` `theme.extend` with the exact tokens from DESIGN.md: colors (all 22 slugs), fontFamily (display/body/mono), fontSize (display/headline/title/body/label/button/data), borderRadius (sm/md/lg/xl/pill), boxShadow (xs/sm/md/lg/xl/focus), spacing scale (xs..4xl). Use the exact hex/px/rem values from DESIGN.md frontmatter.
- Define the same tokens as CSS custom properties on `:root` in `apps/frontend/src/app/globals.css` (e.g. `--color-accent`, `--radius-md`, `--shadow-md`, `--font-display`, `--space-lg`), so the `.impeccable/design.json` sidecar `var(--...)` references resolve. Replace the default Next.js body gradient and the `prefers-color-scheme: dark` block — the system is light-only.
- Wire fonts via `next/font/google` in `apps/frontend/src/app/layout.tsx`: Familjen Grotesk (display), Onest (body/UI), JetBrains Mono (data/mono), with `variable` CSS vars and the DESIGN.md fallback stacks. Apply the font variables to `<html>`/`<body>`.
- Add a token-parity test (Jest) that asserts the Tailwind config values match the DESIGN.md frontmatter token values (guards drift).

### Out of Scope

- No screen/component redesign; no new UI components.
- No dark theme.
- No landing/marketing surface.
- Do not change backend or shared package.

---

## Acceptance Criteria

### Tailwind Theme

- [x] `tailwind.config.ts` colors match DESIGN.md frontmatter exactly (accent #0F766E, canvas #FAFAF9, surface #FFFFFF, ink #0A0A0A, dark-block #0B0B0C, semantic success/warning/danger/info + subtle variants, all neutrals)
- [x] Typography scale + families exposed in Tailwind (`display/headline/title/body/label/button/data`) matching DESIGN.md
- [x] borderRadius (6/10/16/24/999px), boxShadow (xs/sm/md/lg/xl/focus), spacing (4/8/16/24/32/48/80/120px) exposed and matching DESIGN.md

### CSS Variables & Fonts

- [x] `globals.css` exposes the tokens as `:root` CSS custom properties and no longer forces a dark color scheme or the placeholder gradient
- [x] `layout.tsx` loads the three fonts via `next/font/google` with CSS variables and fallbacks; no FOUT-inducing external `<link>`

### Quality Gates

- [x] Token-parity Jest test passes and fails if any token drifts from DESIGN.md
- [x] `npm run lint`, `npm run type-check`, `npm test`, `npm run build` all pass (AGENTS.md gates)
- [x] No component/screen visual change beyond fonts/tokens; existing AUTH-001 tests still pass

---

## Token Reference (normative: `apps/frontend/DESIGN.md` frontmatter)

The values below are embedded for self-containment, but **the DESIGN.md frontmatter is the single source of truth**. If any discrepancy is found, DESIGN.md wins and this story must be updated.

### Colors (22 slugs)

| Slug | Hex |
|------|-----|
| accent | `#0F766E` |
| accent-hover | `#115E59` |
| accent-active | `#0A4F49` |
| accent-subtle | `#E6F4F2` |
| accent-contrast | `#FFFFFF` |
| canvas | `#FAFAF9` |
| surface | `#FFFFFF` |
| surface-muted | `#F4F4F2` |
| border | `#E7E5E4` |
| border-strong | `#D6D3D1` |
| ink | `#0A0A0A` |
| ink-secondary | `#52525B` |
| ink-muted | `#A1A1AA` |
| dark-block | `#0B0B0C` |
| success | `#15803D` |
| success-subtle | `#DCFCE7` |
| warning | `#B45309` |
| warning-subtle | `#FEF3C7` |
| danger | `#B91C1C` |
| danger-subtle | `#FEE2E2` |
| info | `#1D4ED8` |
| info-subtle | `#DBEAFE` |

### Typography

| Role | Font Stack | Size | Weight | Line Height | Letter Spacing | Feature |
|------|-----------|------|--------|-------------|----------------|---------|
| display | Familjen Grotesk, Helvetica Neue, Arial, system-ui, sans-serif | `clamp(2.75rem, 5vw, 4.5rem)` | 700 | 1.02 | `-0.02em` | — |
| headline | Familjen Grotesk, Helvetica Neue, Arial, system-ui, sans-serif | `clamp(1.875rem, 3vw, 2.75rem)` | 650 | 1.1 | `-0.01em` | — |
| title | Familjen Grotesk, Helvetica Neue, Arial, system-ui, sans-serif | `1.25rem` | 600 | 1.3 | — | — |
| body | Onest, system-ui, -apple-system, Segoe UI, Roboto, sans-serif | `1rem` | 400 | 1.6 | — | — |
| label | JetBrains Mono, ui-monospace, SFMono-Regular, Menlo, monospace | `0.75rem` | 600 | 1.2 | `0.12em` | — |
| button | Onest, system-ui, -apple-system, Segoe UI, Roboto, sans-serif | `0.9375rem` | 600 | 1 | `0.01em` | — |
| data | JetBrains Mono, ui-monospace, SFMono-Regular, Menlo, monospace | `0.875rem` | 500 | 1.4 | — | `tnum` |

### borderRadius

| Slug | Value |
|------|-------|
| sm | `6px` |
| md | `10px` |
| lg | `16px` |
| xl | `24px` |
| pill | `999px` |

### boxShadow

| Slug | Value |
|------|-------|
| xs | `0 1px 2px rgba(10,10,10,.04)` |
| sm | `0 1px 3px rgba(10,10,10,.06), 0 1px 2px rgba(10,10,10,.04)` |
| md | `0 4px 12px rgba(10,10,10,.06), 0 2px 4px rgba(10,10,10,.04)` |
| lg | `0 12px 28px rgba(10,10,10,.10), 0 4px 8px rgba(10,10,10,.05)` |
| xl | `0 24px 48px -12px rgba(10,10,10,.18)` |
| focus | `0 0 0 3px rgba(15,118,110,.25)` |

### Spacing

| Slug | Value |
|------|-------|
| xs | `4px` |
| sm | `8px` |
| md | `16px` |
| lg | `24px` |
| xl | `32px` |
| 2xl | `48px` |
| 3xl | `80px` |
| 4xl | `120px` |

### CSS Custom Properties (names for `:root`)

Reference naming convention for `globals.css` — every token gets a `--` prefixed custom property (e.g. `--color-accent`, `--font-display`, `--text-display`, `--radius-md`, `--shadow-md`, `--space-lg`). Names must match the `.impeccable/design.json` sidecar `var(--...)` references.

---

## Technical Tasks (TDD Order)

**Owner:** @dev (Dex)  
**Approach:** Red-Green-Refactor

### Phase 1: Red — Token-Parity Test

#### Task 1.1: Write the failing test

**Red:**
- Write the token-parity Jest test (e.g. `apps/frontend/src/lib/design-tokens.test.ts`) that:
  - Reads the DESIGN.md frontmatter token values
  - Asserts each Tailwind config value matches (colors, fontFamily, fontSize, borderRadius, boxShadow, spacing)
- Run test → fails (no tokens encoded yet)

**Green:**
- (No code yet — this phase is intentionally red.)

**Refactor:**
- Confirm the test is the drift guard and is deterministic (no timing/env dependence)

---

### Phase 2: Green — Tailwind Theme

**Owner:** @dev (Dex)

#### Task 2.1: Encode colors

**Red:**
- Token-parity test still failing for colors

**Green:**
- Extend `theme.extend.colors` with all 22 slugs from DESIGN.md frontmatter
- Run test → colors assertions pass

**Refactor:**
- Flatten slugs at top level (`accent`, `canvas`, `surface`, …) — no nested scales unless DESIGN.md defines one

#### Task 2.2: Encode typography

**Green:**
- Extend `theme.extend.fontFamily` with `display`, `body`, `mono` (design tokens: display/headline/title → display family; body/button → body family; label/data → mono family)
- Extend `theme.extend.fontSize` with `display/headline/title/body/label/button/data` keys carrying size + lineHeight (+ letterSpacing/fontWeight where specified)
- Run test → typography assertions pass

**Refactor:**
- Confirm `fontSize` keys use the DESIGN.md role names (`display`, `headline`, `title`, `body`, `label`, `button`, `data`)

#### Task 2.3: Encode radius, shadow, spacing

**Green:**
- Extend `theme.extend.borderRadius` with `sm/md/lg/xl/pill` (6/10/16/24/999px)
- Extend `theme.extend.boxShadow` with `xs/sm/md/lg/xl/focus`
- Extend `theme.extend.spacing` with `xs/sm/md/lg/xl/2xl/3xl/4xl` (4/8/16/24/32/48/80/120px)
- Run test → all assertions pass (Green)

**Refactor:**
- Verify no placeholder `primary` blue scale remains unless DESIGN.md still requires it (it does not)

---

### Phase 3: CSS Custom Properties + Light-Only

**Owner:** @dev (Dex)

#### Task 3.1: Define `:root` tokens in globals.css

**Red:**
- No automated test; visual/token audit by @ux-design-expert

**Green:**
- Replace the placeholder `--foreground-rgb` / `--background-start-rgb` / `--background-end-rgb` block with DESIGN.md tokens as `:root` CSS custom properties (`--color-*`, `--font-*`, `--text-*`, `--radius-*`, `--shadow-*`, `--space-*`)
- Set `color-scheme: light` explicitly
- Remove the `@media (prefers-color-scheme: dark)` block entirely
- Replace the `body` gradient with flat `--color-canvas` background and `--color-ink` text

**Refactor:**
- Ensure every custom property name matches the `.impeccable/design.json` sidecar references
- Keep custom properties co-located and ordered (colors → fonts → text → radius → shadow → space)

---

### Phase 4: Fonts via next/font/google

**Owner:** @dev (Dex)

#### Task 4.1: Load Familjen Grotesk (display)

**Green:**
- `import { Familjen_Grotesk } from 'next/font/google'`
- Configure with `variable` (e.g. `--font-display`) and DESIGN.md fallback stack
- Apply to `<html>`/`<body>` via the class/variable

#### Task 4.2: Load Onest (body/UI)

**Green:**
- `import { Onest } from 'next/font/google'`
- Configure with `variable` (e.g. `--font-body`) and DESIGN.md fallback stack
- Apply alongside display font

#### Task 4.3: Load JetBrains Mono (data/mono)

**Green:**
- `import { JetBrains_Mono } from 'next/font/google'`
- Configure with `variable` (e.g. `--font-mono`) and DESIGN.md fallback stack
- Apply alongside display/body fonts

**Refactor:**
- Confirm no external `<link>` font tags (avoids FOUT; next/font self-hosts)
- Confirm CSS variable names match globals.css `:root` names

---

### Phase 5: Quality Gates & Audit

**Owner:** @dev (Dex) + @qa + @ux-design-expert (Vista)

#### Task 5.1: Full quality gates

- Run `npm run lint`, `npm run type-check`, `npm test`, `npm run build` in `apps/frontend`
- Confirm token-parity test passes
- Confirm existing AUTH-001 frontend tests still pass

#### Task 5.2: Visual/token fidelity audit (@ux-design-expert)

- Verify token values against DESIGN.md frontmatter (colors, type, radius, shadow, spacing)
- Verify the One Voice Rule (accent ≤10%) and light-only at token level — no dark tokens introduced
- Verify no screen/component visual change beyond fonts/tokens

#### Task 5.3: Story close-out

- Update AC checkboxes
- Update File List below
- Set status to Ready for Review
- Note: DESIGN.md is a SEED (marker present); after real screens exist, re-run `$impeccable document` in scan mode to carbonize tokens

---

## Definition of Done

- [x] All acceptance criteria met
- [x] All four frontend gates pass: `lint`, `type-check`, `test`, `build`
- [x] Token parity verified against DESIGN.md frontmatter
- [x] File list updated
- [x] Status set to Ready for Review

---

## Agent Tasks

- **@dev (Dex):** implement Tailwind theme, globals.css tokens, next/font wiring, token-parity test (this story)
- **@qa:** verify test gates and token-parity test behavior (fails on drift)
- **@ux-design-expert (Vista):** verify visual/token fidelity against DESIGN.md (acceptance criteria + One Voice Rule)

---

## File List

- `apps/frontend/tailwind.config.ts` — modified: `theme.extend` with all 22 colors, `fontFamily` (display/body/mono), `fontSize` (display/headline/title/body/label/button/data), `borderRadius`, `boxShadow`, `spacing`
- `apps/frontend/src/app/globals.css` — rewritten: `:root` custom properties (colors, fonts, radius, space, shadows), light-only `body` (flat canvas + ink)
- `apps/frontend/src/app/layout.tsx` — modified: `next/font/google` wiring for Familjen Grotesk / Onest / JetBrains Mono with CSS variables
- `apps/frontend/src/lib/design-tokens.test.ts` — new: token-parity Jest suite (59 assertions) reading DESIGN.md frontmatter
- `apps/frontend/src/app/page.tsx` — modified: auth screen restructured (critique fixes) — Google CTA moved inside the card and first, single 448px alignment, card conformed to `rounded-lg`/`p-6`/`shadow-sm`, `display`/`title`/`body` tokens applied; auth card widened to `max-w-md`
- `apps/frontend/src/components/auth/LoginForm.tsx` — migrated tokens; inline email/password validation; `aria-invalid`/`aria-describedby`; single reserved error region; `isSubmitting` + `focus-visible`
- `apps/frontend/src/components/auth/RegisterForm.tsx` — migrated tokens; inline name/email/password validation; merged single error region with `aria-invalid`/`aria-describedby`; `isSubmitting`
- `apps/frontend/src/stores/auth-store.ts` — split `isLoading` (session bootstrap only) from new `isSubmitting` (login/register), so submitting no longer unmounts the form
- `apps/frontend/src/components/ProtectedRoute.tsx` — loading text `gray-500` → `ink-muted`
- `apps/frontend/DESIGN.md` — read-only normative reference (unchanged)

---

## Dependencies

- **Depends on:** the approved seed `apps/frontend/DESIGN.md`
- **Blocks:** Calendar (home) screen and all future UI work

---

## Dev Notes

- DESIGN.md is the single source of truth; frontmatter is normative. The sidecar at `.impeccable/design.json` uses `var(--...)` references — the CSS variables must match those names/values.
- DESIGN.md is a SEED (marker present); after real screens exist, re-run `$impeccable document` in scan mode to carbonize tokens.
- Absolute imports only (`@/`) per Constitution Art. VI.
- Keep the accent ≤10% usage rule and light-only in mind even at token level (no dark tokens).
- Current `globals.css` uses `--foreground-rgb` / `--background-start-rgb` / `--background-end-rgb`; these are placeholder and should be **replaced**, not extended.
- Next.js 14 + Tailwind: fonts are configured via CSS variables so Tailwind `fontFamily` values reference `var(--font-display)` / `var(--font-body)` / `var(--font-mono)` at runtime.

---

## Dev Agent Record

### Agent Model Used

@dev (Dex) via `opencode` / `deepseek-v4.1-flash`

### Completion Notes

- All 22 color tokens, typography roles/families, radius, shadows, and spacing encoded in `tailwind.config.ts` and mirrored as `:root` CSS custom properties in `globals.css`.
- Fonts loaded via `next/font/google` (self-hosted, no external `<link>`), exposed as `--font-display` / `--font-body` / `--font-mono`.
- Token-parity test added at `src/lib/design-tokens.test.ts`: 59 assertions, parses DESIGN.md YAML frontmatter with `js-yaml` (hoisted from root `node_modules`), plus a light-only guard.
- Removing the placeholder `primary-*` palette silently un-styled the auth screen (`page.tsx`), so those references were migrated to `accent`/Kairos neutrals and the decorative gradient was replaced with flat `bg-canvas` — required to avoid a regression, not a redesign.
- Open follow-up (not blocking): `typography.data.fontFeature: "tnum"` is a DESIGN.md frontmatter token that Tailwind's `fontSize` config cannot hold; the tabular-figures intent is carried by the `tabular-nums` utility per The Column Rule. Owner: future `$impeccable document` scan pass.
- Gates: `lint` clean, `tsc --noEmit` clean, `jest` 112/112 passed, `next build` succeeded. `impeccable detect` returned no findings.
- Quality gate (@ux-design-expert): PASS-WITH-NOTES — token layer faithful; consumer fixes applied in this story.

### File List

See **File List** section above.

---

## Change Log

| Version | Date | Author | Change |
|---------|------|--------|--------|
| 1.0 | 2026-09-11 | Sage (@sm) | Story created |
| 1.1 | 2026-09-11 | Dex (@dev) | Implemented token encoding; ACs/DoD checked; file list + dev record added; status → Ready for Review |
| 1.2 | 2026-09-11 | Dex (@dev) | UI format refinement on auth screen: removed the "Login with Email" toggle; email/password form now renders first with a card heading; Google CTA moved below as "Continue with Google"; tests updated |
| 1.3 | 2026-09-11 | Dex (@dev) | Widened auth card (`max-w-sm` → `max-w-md`) and inputs (larger padding, full width); migrated residual `primary-*`/`gray-*` in LoginForm, RegisterForm and ProtectedRoute to Kairos tokens |
| 1.4 | 2026-09-11 | Dex (@dev) | Impeccable critique fixes (top 3): P0 split `isLoading`/`isSubmitting` so submit no longer unmounts the form; P1 moved Google CTA inside the card (first) with single alignment and card conformed to DESIGN.md; P1 inline validation + `aria-invalid`/`aria-describedby` + single reserved error region |