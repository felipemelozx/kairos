# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

- **Frontend:** Next.js 14, React 18, TypeScript, Tailwind CSS 3, Zustand, date-fns
- **Backend:** Spring Boot 4, Java 21, PostgreSQL 16, Flyway, Spring Security + OAuth2 + JWT
- **Deploy:** Docker Compose on single VPS (self-hosted)

## Users

Single user (the maintainer). Personal productivity tool — no multi-tenancy, no organizations, no team collaboration in MVP. The user already uses calendars and task tools, feels friction switching between them, cares about time awareness, and works across multiple life/work contexts (study, work, clients).

## Product Purpose

Kairos is a calendar-first time productivity system where **only executed work counts as progress**. It helps the user plan time, track commitments, and measure real execution — all in one place. The core tension it resolves: most productivity tools conflate planned time with actual progress. Kairos separates intention (time blocks + checklists) from reality (execution logs from the timer).

## Positioning

The Review view is the differentiator: planned vs executed time, per day and per project. No neighboring product combines calendar-based planning, inline checklists inside time blocks, a timer as single source of truth, and an honest planned-vs-executed comparison in one system. The timer auto-stops at block end — forgotten timers are impossible.

## Operating Context

- Personal daily use across multiple contexts (study, work, clients)
- Self-hosted on a single VPS via Docker Compose
- Authenticated via Google OAuth2 or email/password (JWT in httpOnly cookies)
- Four core screens: Calendar (home), Review, Kanban (aggregating view), Projects

## Capabilities and Constraints

### Confirmed capabilities
- Calendar with Today / Week / Month views
- Time blocks (single occurrences) and recurring series (daily, daily-interval, weekly-weekdays)
- Series materialize occurrences up to 12-month horizon; roll-forward job generates new ones
- Forward edit splits series; per-occurrence override detaches single blocks
- Inline checklists inside each time block (not standalone tasks)
- Timer per block with auto-stop at block end; creates immutable Work Session on stop
- Overtime captured via one-tap orphan log (no timeBlockId, still tied to projectId)
- Kanban as aggregating view over all checklist items (not a separate entity)
- Projects as life/work contexts with color, status, total executed time
- Review: planned vs executed per day and per project, consistency (active days)

### Confirmed constraints
- MVP: simple recurrence only (no full RRULE, no monthly-on-day-N)
- No multi-tenancy, no roles, no team features
- No AI suggestions or automation
- No mobile optimization in MVP
- No Redis, no blue-green, no multi-instance
- Work sessions are immutable and use physical deletion
- History is immutable — editing recurring series never rewrites the past

## Brand Commitments

- Name: **Kairos**
- Tagline: "Only executed work counts as progress"
- Subtitle: "Time-Centered Productivity System"
- Design reference (non-binding): SaaS moderno, clean, fintech-friendly — baseado em arquivo de referência do usuário (`creem.io` style: fundo branco/off-white, cards com sombras suaves, zig-zag layout, mockups reais de produto, badges de status, mascotes ilustrados SVG, blocos de código estilizados). Referência não vinculante — flexibilidade mantida para ajustes.

## Evidence on Hand

- PRD completo: `docs/prd.md`
- Story AUTH-001 (login/registro): `docs/stories/AUTH-001-login.md`
- Backend architecture: `docs/architecture/backend-architecture.md`
- Data model diagrams: `docs/diagrams/data-model.md`
- Design reference file: `/home/felipemelozx/obsidia/life/04-estudos/2026-09-04-design-system-referencia-saas.md`
- No testimonials, case studies, press mentions, or customer data exist — do not fabricate
- No logo or brand assets confirmed beyond the name "Kairos"

## Product Principles

1. **Time blocks are intentions, not execution** — only the timer produces truth
2. **Execution logs are the single source of truth** — immutable, physical deletion, honest by design
3. **Time is the central axis** — every entity orbits around time blocks and work sessions
4. **History is immutable** — recurring series edits never rewrite the past
5. **Honesty over motivation** — unexecuted blocks produce zero progress; no gamification to mask it

## Accessibility & Inclusion

No product-specific accessibility requirement established beyond standard web best practices.
