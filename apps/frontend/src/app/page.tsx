'use client';

import { useState } from 'react';
import { LoginForm } from '@/components/auth/LoginForm';
import { RegisterForm } from '@/components/auth/RegisterForm';
import { useAuthStore } from '@/stores/auth-store';

export default function HomePage() {
  const [showRegister, setShowRegister] = useState(false);
  const { user, isLoading, logout } = useAuthStore();

  if (isLoading) {
    return (
      <main className="min-h-screen bg-canvas flex items-center justify-center">
        <p className="text-ink-muted" role="status">
          Loading...
        </p>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-canvas">
      <div className="container mx-auto px-4 py-16">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="font-display text-display text-ink mb-4">Kairos</h1>
          <p className="text-title text-ink-secondary mb-2">Time-Centered Productivity System</p>
          <p className="text-body text-ink-muted mb-12">
            Only executed work counts as progress
          </p>

          {user ? (
            <div className="mx-auto max-w-md rounded-lg bg-surface p-6 shadow-sm">
              <p className="text-lg font-medium text-ink mb-1">{user.name}</p>
              <p className="text-sm text-ink-secondary mb-6">{user.email}</p>
              <button
                type="button"
                onClick={() => logout()}
                className="w-full rounded-md border border-border px-4 py-2 font-medium text-ink-secondary transition-colors hover:bg-surface-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
              >
                Logout
              </button>
            </div>
          ) : (
            <div className="mx-auto max-w-md rounded-lg bg-surface p-6 text-left shadow-sm">
              <h2 className="font-display text-title text-ink mb-6">
                {showRegister ? 'Create your account' : 'Sign in to Kairos'}
              </h2>

              <a
                href="/api/oauth2/authorization/google"
                className="flex w-full items-center justify-center gap-3 rounded-md border border-border bg-surface px-4 py-2.5 font-medium text-ink-secondary transition-colors hover:bg-surface-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
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
                Continue with Google
              </a>

              <div className="my-6 flex items-center gap-3">
                <span className="h-px flex-1 bg-border" />
                <span className="text-sm text-ink-muted">or</span>
                <span className="h-px flex-1 bg-border" />
              </div>

              {showRegister ? (
                <RegisterForm onSwitchToLogin={() => setShowRegister(false)} />
              ) : (
                <LoginForm onSwitchToRegister={() => setShowRegister(true)} />
              )}
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
