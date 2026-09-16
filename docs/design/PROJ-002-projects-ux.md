# PROJ-002 — Projects UI: UX, Interaction & Accessibility Spec

**Surface:** Projects (`/projects`, Operate mode)
**Design source:** `apps/frontend/DESIGN.md` (The Ledger of Lived Time)
**Story:** `docs/stories/PROJ-002-projects-ui.md` (Resolved Decisions D1–D6)
**Error pattern source:** `docs/design/AUTH-002-error-ux.md`
**Language:** English (current UI language; no i18n framework introduced)

---

## 1. Objective & scope

The Projects screen lets an authenticated user **list, create, edit, archive, reactivate and soft-delete** their projects. It is the first domain UI on top of the `PROJ-001` backend and the first Operate screen after Auth/Design.

Per **D1**, this slice covers **project management only**. It does **not** render a project's tasks, time blocks, execution logs or executed time, and it does **not** add a project detail view (`/projects/{id}`). Those need unimplemented entities and are deferred.

**Design intent:** this is an Operate screen — dense, calm, quiet. `DESIGN.md`'s marketing zig-zag must not leak here. The accent is scarce (One Voice Rule, ≤10%); project colors are *user data* (D4) and are rendered as small markers, never as chrome.

**What this doc is for:** a normative reference for @dev to implement `ProjectList`, `ProjectCard`, `ProjectForm`, `ProjectDeleteDialog` and the `/projects` route, and the baseline for Vista's post-implementation visual/token/a11y audit.

---

## 2. Screen layout — `/projects`

Route: `apps/frontend/src/app/projects/page.tsx` (`'use client'`), wrapped in `ProtectedRoute` (D2). Content renders only after auth resolves (see §9).

```
┌─ main: min-h-screen bg-canvas ───────────────────────────────────┐
│  container: mx-auto w-full max-w-[1200px] px-6 md:px-8 py-12     │
│                                                                  │
│  ┌─ header: flex items-start justify-between gap-6 mb-8 ──────┐  │
│  │  h1 "Projects"          font-display text-headline text-ink │  │
│  │  p  "Organize your…"    text-body text-ink-secondary mt-2   │  │
│  │                          [ New project ]  (primary button)  │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─ ProjectForm (inline panel; only when mode ≠ null) ─────────┐ │
│  │  bg-surface border border-border rounded-lg p-6 shadow-sm   │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌─ state region ──────────────────────────────────────────────┐ │
│  │  loading | error+Retry | empty | populated list             │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

- **Page shell:** `min-h-screen bg-canvas text-ink`.
- **Container:** `mx-auto w-full max-w-[1200px] px-6 md:px-8` — 1200px is DESIGN.md's fixed product container; gutters step 24 → 32px. No zig-zag, no fluid marketing canvas.
- **Header:** `flex items-start justify-between gap-6 mb-8`. Title `font-display text-headline text-ink`; supporting line `mt-2 text-body text-ink-secondary`.
- **Primary action:** `New project`, DESIGN `button-primary` shape — `rounded-md bg-accent px-4 py-2.5 font-medium text-accent-contrast transition-colors hover:bg-accent-hover active:bg-accent-active focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40 disabled:cursor-not-allowed disabled:opacity-60` (mirrors `LoginForm`).
- **List grid:** `grid gap-6 sm:grid-cols-2 lg:grid-cols-3` as a `<ul>` with `<li>` items. 24px gap (spacing `lg`).
- **Back link:** none required. The wordmark/screen is reached from `/`; do not add a nav bar that does not exist yet.

**Spacing note:** existing numeric Tailwind utilities map to the DESIGN scale — `1→4px`, `2→8px`, `4→16px`, `6→24px`, `8→32px`, `12→48px`, `20→80px`, `30→120px`. Use numeric utilities for consistency with the existing codebase; the named scale (`p-lg`, `gap-6`, …) is equivalent.

---

## 3. Project card

Organism: `ProjectCard`. Rendered inside `<li>`; the card itself is an `<article>`.

```
┌─ article: flex flex-col gap-4 rounded-lg border border-border ───┐
│            bg-surface p-6 shadow-sm                              │
│  ┌ row 1: flex items-start justify-between gap-4 ─────────────┐  │
│  │  ● h2 Name                                  [ ACTIVE ]    │  │
│  ├────────────────────────────────────────────────────────────┤  │
│  │  p Description (if present)                                │  │
│  ├ row 2: mt-auto flex items-center justify-between gap-2 ────┤  │
│  │  Created Sep 12, 2026        Edit · Archive · Delete       │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### 3.1 Color marker (user data, not chrome)

- A **dot**, not a fill: `h-2.5 w-2.5 shrink-0 rounded-pill`, colored via inline `style={{ backgroundColor: project.color }}`.
- `aria-hidden="true"` — color is decorative identity here; the project **name** is the accessible identity. The color is also editable in the form, so the dot never carries unique meaning.
- **Rationale:** project colors are stored user data and do **not** count against The One Voice Rule (D4). Keeping the marker tiny prevents them from ever reading as UI accent. **Do not** use a full-card tint, colored border or colored text.
- Inline `style` is the only sanctioned non-token style in this screen, because the value is runtime user data (validated `^#[0-9A-Fa-f]{6}$`). @dev must not derive any other style from it.

### 3.2 Name & description

- Name: `<h2 className="truncate font-display text-title text-ink">{name}</h2>`. Truncate with `truncate`; the full name stays available via the DOM text (no `title` tooltip needed, but harmless).
- Description: rendered only when non-empty — `<p className="text-body text-ink-secondary">{description}</p>`. Null/blank means no description; render nothing (no placeholder line).

### 3.3 Status badge

| Status | Classes | Visual |
|--------|---------|--------|
| `ACTIVE` | `bg-accent-subtle text-accent` (`badge-accent`) | Verdigris wash pill |
| `ARCHIVED` | `bg-surface-muted text-ink-secondary` (`badge`) | Neutral pill |

- Shape: `inline-flex items-center rounded-pill px-2.5 py-1 font-mono text-label uppercase`.
- Label text: `Active` / `Archived`. **The label is the carrier of status; the badge color is redundant** (a11y: never color alone).
- **Decision point (accent budget):** the story names both `badge` and `badge-accent` but does not map them. This spec maps `ACTIVE → badge-accent` for the same reason DESIGN permits Verdigris Wash on active nav/selected rows — it marks the *current* state, not executed work. Both washes are small areas; combined with the primary button and focus rings the screen stays well under the ≤10% budget. If Vista's audit measures accent above budget on a fully populated screen, the fallback is `ACTIVE → neutral badge` (`bg-surface-muted text-ink`) and this doc is updated. **@dev: implement badge-accent now; do not introduce a third badge style.**

### 3.4 Metadata (timestamp)

- `Created {date}` in `font-mono text-data text-ink-secondary`. Format `Sep 12, 2026` via `Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' })` on `createdAt`.
- **Do not invent metrics.** No task count, no executed time, no "last worked" — those sources do not exist (D1). `createdAt` is the only real datum available.
- **Contrast flag:** use `text-ink-secondary` (#52525B, AA on white). **Do not** use `text-ink-muted` (#A1A1AA) for this text — Faded Graphite is for placeholders/disabled labels only and fails AA for normal text.

### 3.5 Actions

Action row: `mt-auto flex items-center justify-between gap-2` (metadata left, actions right). Buttons are ghost-style text buttons (`button-ghost`): `rounded-md px-2.5 py-1.5 text-button text-ink-secondary transition-colors hover:bg-accent-subtle hover:text-accent focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40`.

| Status | Actions shown |
|--------|---------------|
| `ACTIVE` | `Edit`, `Archive`, `Delete` |
| `ARCHIVED` | `Edit`, `Reactivate`, `Delete` |

- `Delete` uses `text-danger hover:bg-danger-subtle hover:text-danger` (destructive affordance); it is **never** the accent.
- Every action has an accessible name that includes the project name, e.g. `aria-label={`Edit ${name}`}`. Visible label stays the short verb.
- Actions are always reachable by keyboard (no hover-only menu). No kebab menu in this slice.

---

## 4. Create / edit form

Organism: `ProjectForm`, one component with `mode: 'create' | 'edit'`. Rendered as an **inline panel** between the header and the list, controlled by `ProjectList` via `editing: Project | null` and `isCreating: boolean` (D5/D6).

**Why inline, not a modal:** the codebase has no modal primitive; the delete confirmation is the one place a blocking dialog is required (D3). An inline panel avoids a second focus-trap implementation, keeps the form mounted across failed submits, and mirrors the Auth card form. If @dev prefers a modal form, it must follow the delete dialog's focus/ESC rules (§7) — but inline is the recommendation.

```
┌─ section bg-surface border border-border rounded-lg p-6 shadow-sm ─┐
│  h2  "New project" / "Edit project"   font-display text-title      │
│                                                                    │
│  Name *            (count {n}/100, right-aligned, font-mono)       │
│  [ input                                           ]               │
│  inline error (role=alert) when invalid                            │
│                                                                    │
│  Description (optional)      (count {n}/500)                       │
│  [ textarea rows=3                                 ]               │
│  inline error when invalid                                         │
│                                                                    │
│  Color *                                                           │
│  [●][●][●][●][●][●]   [color well]   [ #0F766E   ]                 │
│  inline error when invalid                                         │
│                                                                    │
│  reserved form-level alert line (min-h)                            │
│  [Cancel]                                    [Create project]      │
└────────────────────────────────────────────────────────────────────┘
```

### 4.1 Fields

| Field | Control | Constraints | Notes |
|-------|---------|-------------|-------|
| `name` | `<input type="text">` | required, `maxLength=100` | Trim on validate/submit; `autoComplete="off"` |
| `description` | `<textarea rows={3}>` | optional, `maxLength=500` | Absent/blank = no description |
| `color` | presets + `<input type="color">` + hex text | `^#[0-9A-Fa-f]{6}$`; required on create, optional-on-edit (only-if-changed) | See §5 |

- Labels are real `<label htmlFor>`: `Name`, `Description (optional)`, `Hex color`. The color group has a `fieldset`/`legend` of `Color`.
- Base input classes mirror `AUTH-002`: `w-full rounded-md border bg-surface px-3.5 py-2.5 text-ink placeholder-ink-muted focus:outline-none focus:ring-2` with `border-border focus:border-accent focus:ring-accent/20`, and invalid `border-danger focus:border-danger focus:ring-danger/20`.
- Character counters (`{n}/100`, `{n}/500`) sit in the label row, right-aligned, `font-mono text-data text-ink-secondary`. Counters are informational; the error message is the source of truth. `maxLength` on the control caps typed input at the bound.

### 4.2 Validation (mirrors the DTOs and D5)

Validate all fields **on submit**; on client validation failure do **not** call the store/API. Clear a field's error on the first edit of that field; clear the form-level error on any edit (identical timing to `AUTH-002`).

| Field | Condition | Error message | Error id |
|-------|-----------|---------------|----------|
| `name` | empty after trim | `Enter a project name.` | `project-name-error` |
| `name` | length > 100 | `Name must be 100 characters or fewer.` | `project-name-error` |
| `description` | length > 500 | `Description must be 500 characters or fewer.` | `project-description-error` |
| `color` | empty **on create** | `Choose a color.` | `project-color-error` |
| `color` | malformed (`^#[0-9A-Fa-f]{6}$` fails, case-insensitive) | `Enter a valid hex color (for example #0F766E).` | `project-color-error` |
| `color` (edit) | empty | *no error* — treated as unchanged, not sent | — |

- Field order for focus: `['name', 'description', 'color']` (`getFirstInvalidField`); `color` focuses the **hex text input**.
- On API `ApiError.details`, map known keys (`name`, `description`, `color`) to inline errors via `pickFieldErrors`; if none match, show the reserved form-level region.
- Form-level fallback (reserved `role="alert" aria-live="polite"`, `min-h-[1.25rem] text-sm text-danger`, `id="project-form-error"`): `Could not save the project. Please try again.`, or the API message passed through verbatim when present.
- Flag: since the whole form is an inline panel, still keep the region **reserved** (`min-h`) so validation does not shift the layout (AUTH-002 §1).
- Submit must send **only changed fields** in edit mode (partial `PATCH`): normalize `description` `null/undefined → ''`; compare against the original; include a field only when its value differs. On create, always send `{ name, description, color }`.

### 4.3 Submit / `isSubmitting`

- Submit label: `Create project` / `Save changes`; while submitting: `Saving...`. The button is `disabled` and `aria-busy` is set on the form (`aria-busy={isSubmitting}`).
- **The form stays mounted** across submit — never unmount it on `isSubmitting` (the `AUTH-002`/`DS-001` regression fix). Field values and errors remain visible. Optionally disable inputs while in flight; do not clear them.
- On success: close the panel (create), or close and update the card (edit); focus returns to the `New project` / edited card's `Edit` button respectively.
- On failure: keep the form open, surface errors per §4.2, move focus to the first invalid field when present.

---

## 5. Color selection (D4)

Three synchronized controls; the **hex text input is the canonical field** (validation + `aria-invalid`/`aria-describedby` target).

```
<fieldset aria-describedby={error ? 'project-color-error' : undefined}>
  <legend>Color</legend>
  [preset] [preset] [preset] [preset] [preset] [preset]   [color well]   [ #0F766E ]
</fieldset>
```

### 5.1 Presets

| Token | Hex | `aria-label` |
|-------|-----|--------------|
| `accent` | `#0F766E` | `Use color #0F766E` |
| `info` | `#1D4ED8` | `Use color #1D4ED8` |
| `success` | `#15803D` | `Use color #15803D` |
| `warning` | `#B45309` | `Use color #B45309` |
| `danger` | `#B91C1C` | `Use color #B91C1C` |
| `dark-block` | `#0B0B0C` | `Use color #0B0B0C` |

These six hexes are exactly the `DESIGN.md` token values required by D4/`PROJECT_COLOR_PRESETS`. **No new palette, no invented defaults.**

- Each preset is `<button type="button" aria-pressed={hexEq(color, preset)}>`.
- Visual: `h-8 w-8 rounded-pill border border-border` with inline `style={{ backgroundColor: preset }}`; `hover:scale-105` is **not** used (no ornamental motion) — hover shifts to `ring-2 ring-surface-muted`; **selected** = `ring-2 ring-accent ring-offset-2 ring-offset-surface` (a shape/ring cue, not color alone) plus `aria-pressed="true"`.
- Normal / hover / focus / selected:

| State | Classes |
|-------|---------|
| normal | `border border-border` |
| hover | `hover:ring-2 hover:ring-border-strong` |
| focus | `focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40` |
| selected | `ring-2 ring-accent ring-offset-2 ring-offset-surface` |
| error (group) | the hex input shows `border-danger …`; presets are not individually marked invalid |

### 5.2 Native color well

- `<input type="color" value={color} aria-label="Pick a custom color">`, `h-10 w-10 rounded-md border border-border bg-surface p-1`.
- Changing it updates the hex input and the selected preset state.
- The native input is not the validation target (its output is always a valid `#rrggbb`); it is a convenience sibling.

### 5.3 Hex input

- `<input type="text" inputMode="text" maxLength={7} placeholder="#0F766E">`, label `Hex color`, canonical target of `project-color-error`.
- Bind the value as typed; validate on submit with `PROJECT_COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/` (case-insensitive).
- Typing a valid hex updates the color well and deselects presets that no longer match; selecting a preset overwrites the hex.

**Accessibility of the group:** native `<fieldset>` + `<legend>` names the group; preset buttons expose `aria-pressed`; the color well has an `aria-label`; the hex input has a visible `<label>`. The selected color is never communicated by color alone — the hex text and `aria-pressed` carry it.

---

## 6. Archive / reactivate (D3)

Two distinct, reversible vs destructive actions. Archive is **not** delete.

### 6.1 Archive (ACTIVE → ARCHIVED)

- **Inline confirmation** inside the card (no dialog). Clicking `Archive` swaps the action row for a compact confirm block:
  ```
  bg-surface-muted rounded-md p-3 mt-2
  "Archive “{name}”?"                 text-body text-ink
  "You can reactivate it later."      text-sm text-ink-secondary
  [Cancel]  [Archive]                 (secondary / danger-neutral)
  ```
  Confirm `Archive` uses `bg-accent text-accent-contrast` (it is a normal state change, not a destructive red). Cancel restores the normal action row. `Escape` and focus-out **do not** auto-confirm.
- On confirm: `PATCH /api/projects/{id}` with `{ status: 'ARCHIVED' }`; button label `Archiving...` + disabled while in flight. On success the card **stays in the list**, badge flips to `Archived`, actions become `Edit / Reactivate / Delete`. On failure, restore the action row, keep the project ACTIVE, show the form-level fallback in the card (`Could not update the project. Please try again.`) as a `role="alert"`.
- Microcopy is honest and reversible: never say "delete", never imply data loss.

### 6.2 Reactivate (ARCHIVED → ACTIVE)

- No confirmation — the change is reversible and non-destructive; `Reactivate` acts immediately.
- `PATCH /api/projects/{id}` with `{ status: 'ACTIVE' }`; label `Reactivating...` while in flight. On success badge flips to `Active`, action becomes `Archive`. On failure the badge stays `Archived` and an inline `role="alert"` shows the fallback.

### 6.3 Listing order

- Render **server order**; do **not** invent client-side sorting or reordering. Archived projects remain listed (no `?status` filter, D5 of `PROJ-001`). No separate "Archived" section in this slice.

---

## 7. Delete (D3)

Organism: `ProjectDeleteDialog` — the only blocking dialog in this slice.

```
┌─ overlay: fixed inset-0 z-50 flex items-center justify-center ───┐
│           bg-ink/40                                                │
│  ┌─ panel role="dialog" aria-modal="true" ────────────────────┐   │
│  │  bg-surface rounded-lg p-6 shadow-xl w-full max-w-sm       │   │
│  │  h2 "Delete project"        font-display text-title text-ink│   │
│  │  p  "“{name}” will be removed from your projects.          │   │
│  │      This can’t be undone in the app."  text-body          │   │
│  │                                     text-ink-secondary     │   │
│  │  [Cancel]                              [Delete]            │   │
│  └────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────┘
```

- **Honesty note:** the backend does a *soft delete*, but the app offers no restore. Copy must state the user-visible truth — removed from the app, not recoverable there — without claiming a hard erasure. Do **not** use the literal string "This permanently removes"; use `This can’t be undone in the app.`
- **Panel:** `w-full max-w-sm rounded-lg bg-surface p-6 shadow-xl` (DESIGN modals use `shadow-xl`; radius `lg`). Overlay `bg-ink/40` is the only scrim.
- **Buttons:** `Cancel` = `button-secondary` (`border border-border bg-surface text-ink hover:bg-surface-muted`); `Delete` = `bg-danger text-accent-contrast hover:bg-danger/90 active:bg-danger/80 disabled:opacity-60`. Focus ring stays the system verdigris ring: `focus-visible:ring-2 focus-visible:ring-accent/40`. No `danger-hover` token exists; the `/90` and `/80` opacity modifiers are the sanctioned equivalent — do not add a red hover hex.
- Labels: `Cancel`, `Delete`; in flight `Deleting...` with the button disabled.
- **Microcopy (destructive):** title `Delete project`; body `“{name}” will be removed from your projects. This can’t be undone in the app.`; use curly quotes around the name. No exclamation, no blame.

### 7.1 Behavior & accessibility

- `role="dialog"`, `aria-modal="true"`, `aria-labelledby="project-delete-title"`, `aria-describedby="project-delete-description"`.
- **Initial focus:** the **Cancel** button (least destructive), not Delete.
- **Focus trap:** Tab/Shift+Tab cycle within the dialog while open; nothing behind it is reachable.
- **Escape** closes the dialog and cancels — **no request**.
- On cancel (button, ESC, or overlay click): close, restore focus to the card's `Delete` trigger button.
- On confirm: `DELETE /api/projects/{id}`; on success remove the card from store state, close the dialog, and move focus to the `<h1 id="projects-heading">` (or the empty-state CTA if the list becomes empty) — the trigger no longer exists. On failure keep the dialog open (or close and surface a list-level `role="alert"`), show the fallback `Could not delete the project. Please try again.`; the project stays in the list.

---

## 8. List states (D6)

`ProjectList` owns the state machine and renders exactly one of four states.

| State | Trigger | Markup | Microcopy |
|-------|---------|--------|-----------|
| **Loading** | `isLoading === true` | `<p role="status" className="py-2xl text-body text-ink-secondary text-center">` | `Loading projects…` |
| **Error** | `error !== null` and not loading | `role="alert"` region + `Retry` button | API message, else `Could not load projects. Please try again.` |
| **Empty** | loaded, `projects.length === 0` | centered card + CTA | heading `No projects yet`; body `Create your first project to organize your time blocks.`; `New project` |
| **Populated** | loaded, `projects.length > 0` | `<ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">` of cards | — |

- **Loading** is a text status, not a spinner-only affordance (matches `page.tsx`/`ProtectedRoute`). Use the ellipsis `…`.
- **Error region:** `rounded-md border border-danger/30 bg-danger-subtle p-4` with `text-body text-danger`; `Retry` is a secondary button and re-invokes `fetchProjects`. Do not blank the screen. If a stale list exists, prefer keeping it visible and showing the error above it.
- **Empty state:** `bg-surface border border-border rounded-lg p-6 shadow-sm text-center`, no illustration (no invented assets), no celebratory content. Title `font-display text-title text-ink`; body `text-body text-ink-secondary`; CTA is the primary `New project` button.
- **No fabricated states:** no skeleton cards with invented metrics, no "0 of 0", no progress.

---

## 9. Navigation & auth (D2)

- **Entry point:** on the authenticated card in `apps/frontend/src/app/page.tsx`, add a **`Projects`** link/button between the user's email and `Logout`, using the primary style (`bg-accent … text-accent-contrast`) or a full-width `button-primary`. It links to `/projects` via `next/link` (`<Link href="/projects">`).
- **No forced post-login redirect.** Authenticating keeps the user on `/`; `/projects` is reached only through this link (`D2`).
- **Protected route:** `/projects` is wrapped in `ProtectedRoute` unchanged.
- **Unauthenticated:** `ProtectedRoute` renders `null` and `router.push('/')`; the page must issue **no** `fetchProjects` request. Recommended structure: `<ProtectedRoute><ProjectsScreen /></ProtectedRoute>`, where `ProjectsScreen` mounts only when a user exists and triggers `fetchProjects` in a mount effect. This guarantees the no-fetch guarantee in the AC/`page.test.tsx`. Do not fetch in the page component before auth resolves.
- **Session bootstrap:** while `ProtectedRoute` shows its `role="status"` `Loading...`, `/projects` renders nothing else. No flash of an empty Projects state before auth is known.

---

## 10. Accessibility summary

- **Landmarks & headings:** exactly one `<main>`; `<h1>Projects` (id `projects-heading`); each card name is an `<h2>`. The list is a real `<ul>`/`<li>`.
- **Keyboard:** every control (primary action, card actions, presets, color well, hex, submit, cancel, dialog buttons) is Tab-reachable and Enter/Space-activatable. No hover-only affordances. Focus never escapes the delete dialog while open; Escape cancels it.
- **Focus visible:** every interactive element gets a visible verdigris ring — `focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40`. Inputs use `focus:border-accent focus:ring-accent/20` (AUTH-002). Never remove the outline without a replacement ring.
- **Error wiring:** `aria-invalid` and `aria-describedby` on every validated field; inline error nodes `id="project-<field>-error"` with `role="alert"`; **one** reserved form-level `role="alert" aria-live="polite"` (`project-form-error`) for non-field errors. Never render the same message twice.
- **Focus management:** focus to the first invalid field on failed submit; initial focus on **Cancel** in the delete dialog; focus returns to the invoking control on close/cancel; after a successful delete, focus moves to the list heading (or empty-state CTA).
- **Contrast:** `text-danger` (#B91C1C) on `surface`/`canvas` and `bg-danger` + white pass AA; `text-ink-secondary` (#52525B) on white/canvas passes AA; `text-ink-muted` is reserved for placeholders and disabled labels only — informational text such as the loading status and the character counters uses `text-ink-secondary` (never `text-ink-muted`). Badge labels (`text-label` uppercase) are text, so their foreground must pass AA — `text-accent` (#0F766E) on `accent-subtle` (#E6F4F2) is AA at this size; `text-ink-secondary` on `surface-muted` is AA.
- **Color is never the sole signal:** status uses a text label; selected preset uses a ring plus `aria-pressed`; the color dot is `aria-hidden`.
- **Live regions:** only the loading status, the list error, the inline field errors and the form-level fallback are announced; nothing else uses `aria-live`.

---

## 11. Microcopy (exact strings, English)

| Location | Condition / state | String |
|----------|-------------------|--------|
| Page | title | `Projects` |
| Page | supporting line | `Organize your time by context.` |
| Header | primary action | `New project` |
| Form | create title | `New project` |
| Form | edit title | `Edit project` |
| Form | name label | `Name` |
| Form | description label | `Description (optional)` |
| Form | color legend | `Color` |
| Form | hex label | `Hex color` |
| Form | name placeholder | `e.g. Deep Work` |
| Form | description placeholder | `What is this project for?` |
| Form | hex placeholder | `#0F766E` |
| Form | create submit / in flight | `Create project` / `Saving...` |
| Form | edit submit / in flight | `Save changes` / `Saving...` |
| Form | cancel | `Cancel` |
| Form | name empty | `Enter a project name.` |
| Form | name > 100 | `Name must be 100 characters or fewer.` |
| Form | description > 500 | `Description must be 500 characters or fewer.` |
| Form | color empty (create) | `Choose a color.` |
| Form | color malformed | `Enter a valid hex color (for example #0F766E).` |
| Form | save fallback | `Could not save the project. Please try again.` |
| Card | action | `Edit` |
| Card | action (active) | `Archive` / in flight `Archiving...` |
| Card | action (archived) | `Reactivate` / in flight `Reactivating...` |
| Card | action | `Delete` |
| Card | metadata | `Created Sep 12, 2026` |
| Archive confirm | prompt / helper | `Archive “{name}”?` / `You can reactivate it later.` |
| Archive confirm | buttons | `Cancel` / `Archive` |
| Delete dialog | title | `Delete project` |
| Delete dialog | impact | `“{name}” will be removed from your projects. This can’t be undone in the app.` |
| Delete dialog | buttons | `Cancel` / `Delete` / in flight `Deleting...` |
| Empty state | heading / body / CTA | `No projects yet` / `Create your first project to organize your time blocks.` / `New project` |
| Loading | status | `Loading projects…` |
| Error | fallback / action | `Could not load projects. Please try again.` / `Retry` |

**Consistency rules:** sentence case; no trailing exclamation; no blame ("Invalid input" ✗) — state the fix; reuse shared strings verbatim; API messages are passed through when present. Project names in confirmations use curly quotes.

---

## 12. Out of scope & follow-ups

- **D1 — deferred:** project detail, tasks, time blocks, execution logs, total executed time, any `/projects/{id}` route or `GET /api/projects/{id}` consumption. Detail stories reuse this list/card foundation.
- No `?status` filter, no search, no pagination, no client-side sorting (not in the story/PRD slice).
- No server-side metrics, counters, "last worked", streaks or progress of any kind.
- No dark theme, no i18n framework, no mobile layout (MVP is graceful collapse only).
- No new palette/radius/shadow: only `DESIGN.md`/`DS-001` tokens. The only non-token style is the runtime project color (user data).
- No animation/mascots/celebration; error and state changes are color-of-stroke/text only, no elevation change.
- Follow-up owner for the badge accent-budget check if a fully populated screen trends over budget — Vista audit; fallback documented in §3.3.

---

## 13. Do / Don't

**Do**
- Keep project colors to a small dot; label status with text as well as color.
- Mirror `AUTH-002` exactly for validation, ids, `aria-*`, focus order and clearing-on-edit.
- Keep the form mounted through `isSubmitting`; send only changed fields on edit.
- Reserve space for the form-level alert so nothing jumps.
- Degrade gracefully: loading text, error + Retry, honest empty state.
- Trap focus in the delete dialog, start on Cancel, ESC cancels with no request.

**Don't**
- Don't fill cards/rows with the project color or use it as chrome.
- Don't use the accent for planned/unexecuted content or for the Delete action.
- Don't invent metrics, sorting, filters or a detail view.
- Don't use `text-ink-muted` for essential text; don't rely on color alone.
- Don't add a red hover token, dark variants, or new radii/shadows.
- Don't claim hard/permanent erasure in the delete copy.

---

*Prepared by @ux-design-expert (Vista) from `DESIGN.md`, `AUTH-002-error-ux.md`, `DS-001-design-tokens.md` and `PROJ-002-projects-ui.md`. Operate mode: scanability, consistency and accessibility outrank expression.*
