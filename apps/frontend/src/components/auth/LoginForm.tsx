'use client';

import { useRef, useState } from 'react';
import { ApiError } from '@/lib/api';
import {
  LoginErrors,
  LoginField,
  getFirstInvalidField,
  hasErrors,
  pickFieldErrors,
  validateLogin,
} from '@/lib/auth-validation';
import { useAuthStore } from '@/stores/auth-store';

interface LoginFormProps {
  onSwitchToRegister: () => void;
  initialEmail?: string;
  initialPassword?: string;
  onDraftChange?: (draft: { email: string; password: string }) => void;
}

const LOGIN_FIELD_ORDER: LoginField[] = ['email', 'password'];

const inputBase =
  'input-nb text-ink focus:outline-none';
const inputDefault = '';
const inputInvalid = 'border-danger';

export function LoginForm({
  onSwitchToRegister,
  initialEmail = '',
  initialPassword = '',
  onDraftChange,
}: LoginFormProps) {
  const [email, setEmail] = useState(initialEmail);
  const [password, setPassword] = useState(initialPassword);
  const [fieldErrors, setFieldErrors] = useState<LoginErrors>({});
  const [formError, setFormError] = useState('');
  const { login, isSubmitting } = useAuthStore();

  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const fieldRefs = { email: emailRef, password: passwordRef } as const;

  const clearFieldError = (field: LoginField) => {
    setFieldErrors((prev) => {
      if (!prev[field]) {
        return prev;
      }
      return { ...prev, [field]: undefined };
    });
    if (formError) {
      setFormError('');
    }
  };

  const focusFirstInvalid = (errors: LoginErrors) => {
    const firstInvalid = getFirstInvalidField(errors, LOGIN_FIELD_ORDER);
    if (firstInvalid) {
      fieldRefs[firstInvalid].current?.focus();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');

    const errors = validateLogin({ email, password });
    setFieldErrors(errors);

    if (hasErrors(errors)) {
      focusFirstInvalid(errors);
      return;
    }

    try {
      await login({ email, password });
    } catch (err) {
      if (err instanceof ApiError) {
        const details = pickFieldErrors(err.details, LOGIN_FIELD_ORDER);
        if (hasErrors(details)) {
          setFieldErrors(details);
          focusFirstInvalid(details);
          return;
        }
      }
      setFormError(err instanceof Error ? err.message : 'Could not sign in. Please try again.');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      <div>
        <label htmlFor="login-email" className="block text-sm font-medium text-ink-secondary mb-1">
          Email
        </label>
        <input
          ref={emailRef}
          id="login-email"
          name="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            onDraftChange?.({ email: e.target.value, password });
            clearFieldError('email');
          }}
          required
          aria-invalid={Boolean(fieldErrors.email)}
          aria-describedby={fieldErrors.email ? 'login-email-error' : undefined}
          className={`${inputBase} ${fieldErrors.email ? inputInvalid : inputDefault}`}
          placeholder="you@example.com"
        />
        {fieldErrors.email && (
          <p id="login-email-error" role="alert" className="mt-1 text-sm text-danger">
            {fieldErrors.email}
          </p>
        )}
      </div>
      <div>
        <label htmlFor="login-password" className="block text-sm font-medium text-ink-secondary mb-1">
          Password
        </label>
        <input
          ref={passwordRef}
          id="login-password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value);
            onDraftChange?.({ email, password: e.target.value });
            clearFieldError('password');
          }}
          required
          aria-invalid={Boolean(fieldErrors.password)}
          aria-describedby={fieldErrors.password ? 'login-password-error' : undefined}
          className={`${inputBase} ${fieldErrors.password ? inputInvalid : inputDefault}`}
          placeholder="Enter your password"
        />
        <div className="mt-2 flex justify-end">
          <button
            type="button"
            title="Password recovery is not available in MVP — use Google sign-in or contact the maintainer"
            aria-label="Account recovery — not available in MVP"
            className="text-sm font-medium text-ink-secondary underline-offset-2 hover:text-ink hover:underline focus:outline-none focus-visible:underline"
          >
            Forgot password?
          </button>
        </div>
        {fieldErrors.password && (
          <p id="login-password-error" role="alert" className="mt-1 text-sm text-danger">
            {fieldErrors.password}
          </p>
        )}
      </div>
      <p
        id="login-error"
        role="alert"
        aria-live="polite"
        className="min-h-[1.25rem] text-sm text-danger"
      >
        {formError}
      </p>
      <button
        type="submit"
        disabled={isSubmitting}
        className="btn-persuade min-h-[52px] w-full text-base disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isSubmitting ? 'Logging in...' : 'Login'}
        <span aria-hidden="true">→</span>
      </button>
      <p className="text-center text-sm text-ink-secondary">
        Don&apos;t have an account?{' '}
        <button
          type="button"
          onClick={onSwitchToRegister}
          className="btn-nb-secondary mt-2 block w-full text-sm"
        >
          Create an account
        </button>
      </p>
    </form>
  );
}
