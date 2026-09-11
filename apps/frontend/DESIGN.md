---
name: Kairos
description: A calendar-first time ledger where only executed work counts as progress.
colors:
  accent: "#0F766E"
  accent-hover: "#115E59"
  accent-active: "#0A4F49"
  accent-subtle: "#E6F4F2"
  accent-contrast: "#FFFFFF"
  canvas: "#FAFAF9"
  surface: "#FFFFFF"
  surface-muted: "#F4F4F2"
  border: "#E7E5E4"
  border-strong: "#D6D3D1"
  ink: "#0A0A0A"
  ink-secondary: "#52525B"
  ink-muted: "#A1A1AA"
  dark-block: "#0B0B0C"
  success: "#15803D"
  success-subtle: "#DCFCE7"
  warning: "#B45309"
  warning-subtle: "#FEF3C7"
  danger: "#B91C1C"
  danger-subtle: "#FEE2E2"
  info: "#1D4ED8"
  info-subtle: "#DBEAFE"
typography:
  display:
    fontFamily: "Familjen Grotesk, Helvetica Neue, Arial, system-ui, sans-serif"
    fontSize: "clamp(2.75rem, 5vw, 4.5rem)"
    fontWeight: 700
    lineHeight: 1.02
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Familjen Grotesk, Helvetica Neue, Arial, system-ui, sans-serif"
    fontSize: "clamp(1.875rem, 3vw, 2.75rem)"
    fontWeight: 650
    lineHeight: 1.1
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Familjen Grotesk, Helvetica Neue, Arial, system-ui, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 600
    lineHeight: 1.3
  body:
    fontFamily: "Onest, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "JetBrains Mono, ui-monospace, SFMono-Regular, Menlo, monospace"
    fontSize: "0.75rem"
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: "0.12em"
  button:
    fontFamily: "Onest, system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "0.01em"
  data:
    fontFamily: "JetBrains Mono, ui-monospace, SFMono-Regular, Menlo, monospace"
    fontSize: "0.875rem"
    fontWeight: 500
    lineHeight: 1.4
    fontFeature: "tnum"
rounded:
  sm: "6px"
  md: "10px"
  lg: "16px"
  xl: "24px"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "24px"
  xl: "32px"
  2xl: "48px"
  3xl: "80px"
  4xl: "120px"
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.accent-contrast}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: "10px 18px"
    height: "40px"
  button-primary-hover:
    backgroundColor: "{colors.accent-hover}"
  button-primary-active:
    backgroundColor: "{colors.accent-active}"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: "10px 18px"
    height: "40px"
  button-secondary-hover:
    backgroundColor: "{colors.surface-muted}"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.ink-secondary}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    padding: "10px 14px"
    height: "40px"
  button-ghost-hover:
    backgroundColor: "{colors.accent-subtle}"
    textColor: "{colors.accent}"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "10px 12px"
    height: "40px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "{spacing.lg}"
  badge:
    backgroundColor: "{colors.surface-muted}"
    textColor: "{colors.ink-secondary}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "4px 10px"
  badge-accent:
    backgroundColor: "{colors.accent-subtle}"
    textColor: "{colors.accent}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "4px 10px"
  nav-item:
    backgroundColor: "transparent"
    textColor: "{colors.ink-secondary}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "8px 12px"
  nav-item-active:
    backgroundColor: "{colors.accent-subtle}"
    textColor: "{colors.accent}"
  planned-executed-bar:
    backgroundColor: "{colors.surface-muted}"
    textColor: "{colors.ink}"
    typography: "{typography.data}"
    rounded: "{rounded.pill}"
  timer-chip:
    backgroundColor: "{colors.accent-subtle}"
    textColor: "{colors.accent}"
    typography: "{typography.data}"
    rounded: "{rounded.pill}"
    padding: "6px 12px"
---

<!-- SEED: established with the user before implementation; re-run $impeccable document once there's code to capture the actual tokens and components. -->

# Design System: Kairos

## Overview

**Creative North Star: "The Ledger of Lived Time"**

Kairos is a ledger, not a dashboard. Planned time blocks are entries written in advance; executed work sessions are posted lines that can never be un-written; the Review view is the balance sheet that reconciles the two. The interface should read like a well-kept accounting book — precise, quiet, and trustworthy. Every surface earns its place, and nothing decorative is allowed to imply progress that did not happen.

The system is deliberately light-only. A warm off-white canvas holds crisp white surfaces, separated by hairline stone rules rather than heavy shadows. Near-black ink carries the text. A single verdigris accent appears only where reality has been posted — a running timer, an executed session, a confirmed action — so that color itself becomes a record of what actually occurred. Planned intentions stay neutral, in the same grey as the paper they were written on.

Density is calm, not sparse. Operate screens (Calendar, Review, Kanban, Projects) are information-dense but quiet: tight, legible rows; fixed monospace columns for time; no ornamental motion. The marketing zig-zag — alternating media, generous blocks — belongs to a separate future surface and must never leak into the product. Depth is a response to intent, not a default: surfaces rest flat, then lift only when the user reaches for them.

**Key Characteristics:**
- Light-only ledger aesthetic: off-white canvas, white surfaces, hairline stone rules.
- A single verdigris accent reserved for executed reality, never for decoration.
- Familjen Grotesk for voice, Onest for reading, JetBrains Mono for every unit of time.
- Planned and executed time always rendered in two fixed monospace columns.
- Flat surfaces at rest; elevation appears only on hover, drag, or floating proof.
- Honesty over motivation: no gamification, no fabricated proof, no AI affordances.

## Colors

The palette is a paper-and-ink neutral field with one meaningful accent and a restrained semantic set for honest states.

### Primary
- **Ledger Verdigris** (#0F766E): The sole interaction accent. Used for posted/executed marks, the running timer, primary actions, active navigation, and focus. Its scarcity is what makes it mean "this is real."
- **Deep Verdigris** (#115E59): Hover state for accent-bearing surfaces and buttons.
- **Pressed Verdigris** (#0A4F49): Active/pressed state for accent surfaces.
- **Verdigris Wash** (#E6F4F2): Tinted background for selected rows, ghost-button hover, and quiet accent contexts. Never used as a large fill.
- **Chalk White** (#FFFFFF): Text and icons sitting on an accent fill.

### Secondary
- **Posted Green** (#15803D) on **Posted Green Wash** (#DCFCE7): Success and completed/posted confirmations.
- **Amber Notice** (#B45309) on **Amber Wash** (#FEF3C7): Warning, nearing block end, or unposted overtime.
- **Overrun Red** (#B91C1C) on **Overrun Wash** (#FEE2E2): Danger, destructive confirmation, and timer overrun.
- **Reference Blue** (#1D4ED8) on **Reference Wash** (#DBEAFE): Informational, neutral system notices.

### Tertiary
- **Midnight Slate** (#0B0B0C): The single dark punctuation block allowed per surface — one dark statement on an otherwise light page. Never a theme, never a full-screen mode.

### Neutral
- **Paper Canvas** (#FAFAF9): Default page background.
- **Ledger White** (#FFFFFF): Card, panel, and input surfaces.
- **Faint Stone** (#F4F4F2): Muted surfaces, table stripes, disabled fields, inset tracks.
- **Hairline Stone** (#E7E5E4): Default 1px borders and dividers.
- **Rule Stone** (#D6D3D1): Stronger borders, input strokes, emphasis separators.
- **Carbon Ink** (#0A0A0A): Primary text and headings.
- **Graphite** (#52525B): Secondary text, metadata, labels.
- **Faded Graphite** (#A1A1AA): Placeholder text and disabled labels.

### Named Rules
**The Posted, Not Planned Rule.** Accent and dark fills mark only executed reality — work sessions, the running timer, posted entries. Planned intentions are always neutral. If a block has not been executed, it may not wear the accent.

**The One Voice Rule.** The accent occupies ≤10% of any screen. Its rarity is the point; a screen that is 10% verdigris is loud, and one that is 30% is broken.

## Typography

**Display Font:** Familjen Grotesk (with "Helvetica Neue", Arial, system-ui, sans-serif)
**Body Font:** Onest (with system-ui, -apple-system, "Segoe UI", Roboto, sans-serif)
**Label/Mono Font:** JetBrains Mono (with ui-monospace, SFMono-Regular, Menlo, monospace)

**Character:** A slightly idiosyncratic grotesk gives Kairos a voice without shouting; a humanist sans keeps dense lists readable; monospace turns time into a first-class, scannable datum. The three never trade places.

### Hierarchy
- **Display** (700, clamp(2.75rem, 5vw, 4.5rem), 1.02, -0.02em): Marketing and empty-state hero headlines only. Never inside the Operate screens.
- **Headline** (650, clamp(1.875rem, 3vw, 2.75rem), 1.1, -0.01em): Page titles and major section headers.
- **Title** (600, 1.25rem, 1.3): Card titles, panel headers, and block names.
- **Body** (400, 1rem, 1.6): Prose, descriptions, checklist items. Keep line length to 65–75ch in reading contexts.
- **Label** (600, 0.75rem, 1.2, 0.12em, uppercase): Field labels, column headers, badges, and small metadata that must be scanned.
- **Button** (600, 0.9375rem, 1, 0.01em): All button and control text.
- **Data** (500, 0.875rem, 1.4, tabular-nums): Every duration, timestamp, and count. Always tabular so columns align.

### Named Rules
**The Column Rule.** Planned-vs-executed comparisons are always set in JetBrains Mono with tabular-nums, in two fixed columns. The numbers never reflow, shift, or use proportional figures — the ledger must line up.

## Layout

Layout is a fixed-max-width grid, not a fluid marketing canvas. Content sits in a 1200px container; wide data views (Calendar week, Kanban board) may use 1320px. Gutters step 24px → 32px → 40px across sm/md/lg. The spacing scale is xs 4px, sm 8px, md 16px, lg 24px, xl 32px, 2xl 48px, 3xl 80px, 4xl 120px.

Operate screens are dense and calm: related rows sit 8–16px apart, panels 24–32px apart, sections 48px apart. Calendar, Review, Kanban, and Projects never use a zig-zag composition. Responsive behavior is graceful collapse rather than mobile optimization — MVP has no mobile layout.

The marketing surface is a separate, future composition and follows a zig-zag grammar: 5/7 or 6/6 column splits with media alternating sides, 96px between blocks, one idea per section, and a background rhythm that alternates surface and canvas with exactly one dark block per surface. None of that grammar applies to the product.

Breakpoints: sm 640px, md 768px, lg 1024px, xl 1280px, 2xl 1536px.

## Elevation & Depth

Depth is a hybrid of tonal layering and quiet shadows: hairline borders do most of the structural work, and shadows are reserved as a response to state. Resting surfaces are flat or carry at most `shadow-sm`; `shadow-lg` and `shadow-xl` appear only on hover, drag, and floating proof cards. Focus is signaled with a soft verdigris ring rather than a border change.

### Shadow Vocabulary
- **shadow-xs** (`0 1px 2px rgba(10,10,10,.04)`): Hairline lift for inline chips and inputs.
- **shadow-sm** (`0 1px 3px rgba(10,10,10,.06), 0 1px 2px rgba(10,10,10,.04)`): Default resting card.
- **shadow-md** (`0 4px 12px rgba(10,10,10,.06), 0 2px 4px rgba(10,10,10,.04)`): Raised panels, popovers, dropdowns.
- **shadow-lg** (`0 12px 28px rgba(10,10,10,.10), 0 4px 8px rgba(10,10,10,.05)`): Hover-lifted cards and draggable blocks.
- **shadow-xl** (`0 24px 48px -12px rgba(10,10,10,.18)`): Floating proof cards and modals.
- **shadow-focus** (`0 0 0 3px rgba(15,118,110,.25)`): Focus-visible ring on any interactive element.

### Named Rules
**The Lift-On-Intent Rule.** Surfaces are flat-ish at rest. Shadow-lg and shadow-xl appear only on hover, drag, or a floating proof card. A static screen should look pressed to the page.

## Shapes

The form language is soft and geometric, with radius scaling by the size of the container. Small controls use 6px (sm), buttons and inputs use 10px (md), cards and panels use 16px (lg), large sheets use 24px (xl), and status/timer pills are fully round (999px). Borders are always a single 1px hairline — never doubled, never heavy. There are no sharp 0px corners in the product, and no mixed radius within one component family.

## Components

### Buttons
- **Shape:** Gently rounded (10px), 40px tall, comfortable horizontal padding (18px).
- **Primary:** Ledger Verdigris fill, Chalk White text, Onest 600 at 0.9375rem. Hover deepens to #115E59; active presses to #0A4F49.
- **Secondary:** Ledger White fill with a 1px Rule Stone border and Carbon Ink text; hover shifts to Faint Stone.
- **Ghost:** Transparent fill, Graphite text; hover reveals a Verdigris Wash background with verdigris text.
- **Focus:** Every variant shows `box-shadow: 0 0 0 3px rgba(15,118,110,.25)` on `:focus-visible`.

### Inputs / Fields
- **Style:** Ledger White fill, 1px Rule Stone stroke, 10px radius, 40px tall, Graphite placeholder.
- **Focus:** Stroke shifts to Ledger Verdigris plus the verdigris focus ring; no layout shift.
- **Error / Disabled:** Error uses Overrun Red stroke and Overrun Wash hint text. Disabled uses Faint Stone fill with Faded Graphite text.

### Cards / Containers
- **Corner Style:** 16px (lg).
- **Background:** Ledger White on Paper Canvas, or Faint Stone for muted/inset regions.
- **Shadow Strategy:** `shadow-sm` at rest; `shadow-lg`/`shadow-xl` only per The Lift-On-Intent Rule.
- **Border:** 1px Hairline Stone by default; Rule Stone when the card is the primary focus.
- **Internal Padding:** 24px (lg), dropping to 16px (md) in dense calendar cells.

### Chips / Status Badges
- **Style:** Fully round (999px), JetBrains Mono uppercase at 0.75rem with 0.12em tracking. Neutral badges use Faint Stone with Graphite text; accent badges use Verdigris Wash with verdigris text.
- **State:** Honest states only — Beta, Self-hosted, MVP, NEW. A badge is applied only when the label is true; it is never decorative.

### Navigation
- **Style:** Quiet top bar on Paper Canvas. Wordmark in Familjen Grotesk 600; items in Onest body with Graphite text.
- **Default / Hover / Active:** Default Graphite; hover deepens to Carbon Ink; active gets a Verdigris Wash pill with verdigris text. The active item is the only accent in the nav.

### Planned-vs-Executed bar
The signature comparison component. Two rows — Planned and Executed — each with a fixed label column and a fixed monospace value column, so the numbers align across every row. The planned row and its track are always neutral (Faint Stone / Rule Stone); the executed row's value and fill are the only accent, because only executed time is real. Durations are JetBrains Mono with tabular-nums (The Column Rule).

### Timer chip
The running-timer indicator and the auto-stop promise. A round pill on Verdigris Wash with a small pulsing verdigris dot, the elapsed time in monospace tabular figures, and a muted "auto-stops at HH:MM" label. It is the one place the accent is allowed to persist on screen, because the timer is the single source of truth. When a timer auto-stops, the chip resolves into a posted state rather than celebrating.

## Do's and Don'ts

### Do:
- **Do** reserve Ledger Verdigris (#0F766E) for executed reality: the running timer, posted sessions, primary actions, active nav, and focus.
- **Do** keep surfaces flat at rest and lift them with `shadow-lg`/`shadow-xl` only on hover, drag, or floating proof cards.
- **Do** render every duration, timestamp, and count in JetBrains Mono with tabular-nums in two fixed columns.
- **Do** separate regions with 1px Hairline Stone borders and whitespace before reaching for shadow.
- **Do** keep planned blocks and their tracks neutral, and overlay executed work in accent.
- **Do** label states honestly (Beta, Self-hosted, MVP, NEW) and only when the label is true.
- **Do** keep the accent at or below 10% of any screen (The One Voice Rule).
- **Do** maintain WCAG AA contrast for text and interactive states.

### Don't:
- **Don't** fabricate testimonials, customer logos, press mentions, or pricing — none exist.
- **Don't** add mascots, illustrated characters, or celebratory gamification (confetti, streaks, vanity badges).
- **Don't** imply AI suggestions or automation; Kairos has none.
- **Don't** use the accent to decorate planned, unexecuted content.
- **Don't** introduce a dark theme; the dark block is punctuation only, one per surface.
- **Don't** use zero-radius or arbitrarily mixed corners within a component family.
- **Don't** apply the marketing zig-zag composition to Operate screens.
- **Don't** use proportional figures for time data.
