# AUTH-002 — Error UX, Microcopy & Accessibility Spec

**Surface:** Email Login & Register (Operate mode)
**Design source:** `apps/frontend/DESIGN.md` (The Ledger of Lived Time)
**Story:** `docs/stories/AUTH-002-field-level-validation.md`
**Language:** English (current UI language; no i18n framework introduced)

---

## 1. Error presentation rules

| Error type | Where it appears | Visual | Announcement |
|------------|------------------|--------|--------------|
| Field error (client or server) | Inline, directly under the offending input | Overrun Red text (`text-danger`), input stroke `border-danger` | `role="alert"` on the inline message + `aria-describedby` on the input |
| Form / API error without field details | Once, below the last field, above the submit button | `text-danger`, reserved space to avoid layout shift | `role="alert"` + `aria-live="polite"` |
| Success / loading | Button label change only (`Logging in...` / `Creating account...`) | Existing behavior | Existing behavior |

Rules:
- Never show the same message twice (do not duplicate a field error at form level).
- Reserve vertical space for the form-level message so the form does not jump.
- Field-level errors are the primary mechanism; the form-level message is the fallback.
- Kairos surfaces stay flat at rest — error state changes stroke color and text color only, never elevation.

---

## 2. Microcopy (exact strings)

| Field | Condition | String |
|-------|-----------|--------|
| Login email | empty | `Enter your email address.` |
| Login email | malformed | `Enter a valid email address.` |
| Login password | empty | `Enter your password.` |
| Login (API) | no field details | `Could not sign in. Please try again.` |
| Register name | empty | `Enter your name.` |
| Register name | > 100 chars | `Name must be 100 characters or fewer.` |
| Register email | empty | `Enter your email address.` |
| Register email | malformed | `Enter a valid email address.` |
| Register password | empty | `Enter a password.` |
| Register password | < 8 chars | `Password must be at least 8 characters.` |
| Register (API) | no field details | `Could not create your account. Please try again.` |

Consistency rules:
- Shared conditions use identical strings across Login and Register (`Enter your email address.`, `Enter a valid email address.`).
- Sentence case, no trailing exclamation, no blame ("Invalid input" ✗), state the fix ("Enter a valid email address." ✓).
- API messages are passed through verbatim when present (they are already user-facing).

---

## 3. Visual spec (tokens → Tailwind)

Input, normal:
```
border-border focus:border-accent focus:ring-accent/20
```

Input, error:
```
border-danger focus:border-danger focus:ring-danger/20
```

Inline field error text:
```
mt-1 text-sm text-danger
```

Form-level error (reserved line):
```
min-h-[1.25rem] text-sm text-danger
```

- Base input classes remain unchanged: `w-full rounded-md border bg-surface px-3.5 py-2.5 text-ink placeholder-ink-muted focus:outline-none focus:ring-2`.
- Radius stays `rounded-md` (10px); no new shapes.
- No shadow/`Surface` change on error — stroke + text color only.
- Contrast: `#B91C1C` on `#FFFFFF` and on `#FAFAF9` passes WCAG AA for normal text.

---

## 4. Accessibility spec

- Each input:
  - `aria-invalid={hasFieldError ? 'true' : 'false'}`
  - `aria-describedby={hasFieldError ? '<form>-<field>-error' : undefined}`
- Each inline error node:
  - `id="<form>-<field>-error"`, `role="alert"`
  - Example ids: `login-email-error`, `login-password-error`, `register-name-error`, `register-email-error`, `register-password-error`
- Form-level error node: `id="<form>-error"`, `role="alert"`, `aria-live="polite"`.
- Focus management: on submit, validate all; if invalid, focus the first invalid field in visual order:
  - Login: `email` → `password`
  - Register: `name` → `email` → `password`
- Do not announce the same field error twice: the inline node is the only alert for that field; the form-level alert only carries non-field errors.
- Errors appear only when present, so `role="alert"` fires on appearance rather than on initial render.
- Label remains a real `<label htmlFor>`; error text is neither placeholder nor title.
- Keyboard: submit via Enter continues to work; focus never trapped.

---

## 5. Consistency rules between Login and Register

- Same id scheme (`<form>-<field>-error`) and same aria wiring.
- Same timing: validate on submit; clear a field error on the first edit of that field; clear the form-level error on any edit.
- Same order of focus priority as visual order.
- Same error-tone microcopy.

---

## 6. Do / Don't

**Do**
- Put the error next to the field it belongs to.
- Keep error styling to Overrun Red stroke + text.
- Move focus so keyboard/AT users land on the problem.
- Trim whitespace only for validation; keep existing API payload behavior.

**Don't**
- Don't replace field errors with a single top-of-form message.
- Don't add a validation library or i18n framework.
- Don't add password complexity rules beyond the backend `min = 8`.
- Don't add animation, shake, mascots, or celebratory states.
- Don't use color alone — the text states the fix.

---

*Prepared by @ux-design-expert (Vista) with the `impeccable` skill. Operate mode: scanability, consistency and accessibility outrank expression; the brand stays in precise details.*
