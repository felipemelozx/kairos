'use client';

import { useState } from 'react';
import Link from 'next/link';
import { LoginForm } from '@/components/auth/LoginForm';
import { RegisterForm } from '@/components/auth/RegisterForm';
import { useAuthStore } from '@/stores/auth-store';

export default function HomePage() {
  const [showRegister, setShowRegister] = useState(false);
  const [draft, setDraft] = useState({ name: '', email: '', password: '' });
  const { user, isLoading, logout } = useAuthStore();

  if (isLoading) {
    return (
      <main className="min-h-screen bg-canvas flex items-center justify-center">
        <p className="text-ink-secondary" role="status">
          Loading...
        </p>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-canvas">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex w-full max-w-[1200px] items-center justify-between px-6 py-4">
          <span className="font-display text-title font-semibold text-ink">Kairos</span>
          <nav aria-label="Primary" className="flex items-center gap-3">
            <span className="hidden rounded-pill bg-surface-muted px-2.5 py-1 font-mono text-label uppercase text-ink-secondary sm:inline">
              MVP
            </span>
            {user ? (
              <Link
                href="/projects"
                className="rounded-md px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-surface-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
              >
                Open app
              </Link>
            ) : (
              <a
                href="#signin"
                className="rounded-md px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-surface-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
              >
                Sign in
              </a>
            )}
          </nav>
        </div>
      </header>

      <div className="mx-auto w-full max-w-[1200px] px-6">
        <section aria-labelledby="hero-heading" className="grid gap-10 py-16 md:grid-cols-2 md:items-center md:py-24">
          <div>
            <h1 id="hero-heading" className="font-display text-display uppercase text-ink">
              Kairos
            </h1>
            <p className="mt-2 font-display text-headline text-ink">
              Plan honestly. Prove it with execution.
            </p>
            <p className="mt-2 text-title text-ink-secondary">Time-Centered Productivity System</p>
            <p className="mt-4 max-w-[52ch] text-body text-ink-secondary">
              Only executed work counts as progress. Plan time blocks, run the timer as the
              single source of truth, and reconcile planned vs executed in Review.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <a href="#signin" className="btn-persuade text-base">
                Start tracking <span aria-hidden="true">→</span>
              </a>
              <a
                href="#how-it-works"
                className="inline-flex min-h-[52px] items-center justify-center rounded-pill border-2 border-dark-block bg-surface px-7 py-4 text-base font-semibold text-ink shadow-hard transition-transform hover:-translate-x-px hover:-translate-y-px focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
              >
                How it works
              </a>
            </div>
          </div>

          <div
            id="how-it-works"
            aria-label="Planned versus executed preview"
            className="rounded-2xl border-2 border-dark-block bg-surface p-6 shadow-hard-lg"
          >
            <p className="font-mono text-label uppercase text-ink-secondary">
              Today — planned vs executed
            </p>
            <dl className="mt-4 space-y-4">
              <div className="rounded-xl border border-border bg-canvas p-4">
                <div className="flex items-baseline justify-between gap-4">
                  <dt className="text-sm font-medium text-ink-secondary">Planned</dt>
                  <dd className="font-mono text-data tabular-nums text-ink">04h 30m</dd>
                </div>
                <div
                  className="mt-2 h-2.5 rounded-pill bg-surface-muted"
                  role="img"
                  aria-label="Planned 4 hours 30 minutes"
                >
                  <div className="h-2.5 w-3/4 rounded-pill bg-border-strong" />
                </div>
              </div>
              <div className="rounded-xl border-2 border-dark-block bg-accent-subtle p-4">
                <div className="flex items-baseline justify-between gap-4">
                  <dt className="text-sm font-medium text-accent">Executed</dt>
                  <dd className="font-mono text-data tabular-nums text-accent">02h 15m</dd>
                </div>
                <div
                  className="mt-2 h-2.5 rounded-pill bg-surface"
                  role="img"
                  aria-label="Executed 2 hours 15 minutes"
                >
                  <div className="h-2.5 w-1/2 rounded-pill bg-accent" />
                </div>
              </div>
            </dl>
            <p className="mt-4 font-mono text-data tabular-nums text-ink-secondary">
              Balance —02h 15m · unexecuted blocks count zero
            </p>
          </div>
        </section>

        <section id="signin" aria-label="Sign in" className="pb-24">
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-body text-ink-secondary">
              Only executed work counts as progress
            </p>

            {user ? (
              <div className="mx-auto mt-8 max-w-md rounded-lg border border-border bg-surface p-6 shadow-sm">
                <p className="text-lg font-medium text-ink mb-1">{user.name}</p>
                <p className="text-sm text-ink-secondary mb-6">{user.email}</p>
                <Link
                  href="/projects"
                  className="btn-persuade mb-3 w-full text-base focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
                >
                  Projects <span aria-hidden="true">→</span>
                </Link>
                <button
                  type="button"
                  onClick={() => logout()}
                  className="inline-flex min-h-[40px] w-full items-center justify-center rounded-md border border-border px-4 py-2 font-medium text-ink-secondary transition-colors hover:bg-surface-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
                >
                  Logout
                </button>
              </div>
            ) : (
              <div className="mx-auto mt-8 max-w-md rounded-lg bg-surface p-6 text-left shadow-sm">
                <h2 className="font-display text-title text-ink mb-6">
                  {showRegister ? 'Create your account' : 'Sign in to Kairos'}
                </h2>

                <a
                  href="/api/oauth2/authorization/google"
                  className="btn-persuade w-full text-base"
                >
                  <svg className="h-5 w-5" viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.27-4.74 3.27-8.1Z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23Z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.06H2.18a11 11 0 0 0 0 9.88l3.66-2.84Z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15A11 11 0 0 0 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52Z"
                    />
                  </svg>
                  Continue with Google <span aria-hidden="true">→</span>
                </a>

                <div className="my-6 flex items-center gap-3">
                  <span className="h-px flex-1 bg-border" />
                  <span className="text-sm text-ink-secondary">or continue with email</span>
                  <span className="h-px flex-1 bg-border" />
                </div>

                {showRegister ? (
                  <RegisterForm
                    key="register"
                    onSwitchToLogin={() => setShowRegister(false)}
                    initialName={draft.name}
                    initialEmail={draft.email}
                    initialPassword={draft.password}
                    onDraftChange={(d) => setDraft(d)}
                  />
                ) : (
                  <LoginForm
                    key="login"
                    onSwitchToRegister={() => setShowRegister(true)}
                    initialEmail={draft.email}
                    initialPassword={draft.password}
                    onDraftChange={(d) =>
                      setDraft((prev) => ({ ...prev, email: d.email, password: d.password }))
                    }
                  />
                )}
              </div>
            )}
          </div>
        </section>
      </div>
    </main>
  );
}
