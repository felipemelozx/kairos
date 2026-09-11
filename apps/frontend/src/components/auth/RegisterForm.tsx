'use client';

import { useRef, useState } from 'react';
import { ApiError } from '@/lib/api';
import {
  PASSWORD_MIN_LENGTH,
  RegisterErrors,
  RegisterField,
  getFirstInvalidField,
  hasErrors,
  pickFieldErrors,
  validateRegister,
} from '@/lib/auth-validation';
import { useAuthStore } from '@/stores/auth-store';

interface RegisterFormProps {
  onSwitchToLogin: () => void;
}

const REGISTER_FIELD_ORDER: RegisterField[] = ['name', 'email', 'password'];

const inputBase =
  'w-full rounded-md border bg-surface px-3.5 py-2.5 text-ink placeholder-ink-muted focus:outline-none focus:ring-2';
const inputDefault = 'border-border focus:border-accent focus:ring-accent/20';
const inputInvalid = 'border-danger focus:border-danger focus:ring-danger/20';

export function RegisterForm({ onSwitchToLogin }: RegisterFormProps) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<RegisterErrors>({});
  const [formError, setFormError] = useState('');
  const { register, isSubmitting } = useAuthStore();

  const nameRef = useRef<HTMLInputElement>(null);
  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const fieldRefs = { name: nameRef, email: emailRef, password: passwordRef } as const;

  const clearFieldError = (field: RegisterField) => {
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

  const focusFirstInvalid = (errors: RegisterErrors) => {
    const firstInvalid = getFirstInvalidField(errors, REGISTER_FIELD_ORDER);
    if (firstInvalid) {
      fieldRefs[firstInvalid].current?.focus();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');

    const errors = validateRegister({ name, email, password });
    setFieldErrors(errors);

    if (hasErrors(errors)) {
      focusFirstInvalid(errors);
      return;
    }

    try {
      await register({ name, email, password });
    } catch (err) {
      if (err instanceof ApiError) {
        const details = pickFieldErrors(err.details, REGISTER_FIELD_ORDER);
        if (hasErrors(details)) {
          setFieldErrors(details);
          focusFirstInvalid(details);
          return;
        }
      }
      setFormError(
        err instanceof Error ? err.message : 'Could not create your account. Please try again.'
      );
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      <div>
        <label htmlFor="register-name" className="block text-sm font-medium text-ink-secondary mb-1">
          Name
        </label>
        <input
          ref={nameRef}
          id="register-name"
          name="name"
          type="text"
          autoComplete="name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            clearFieldError('name');
          }}
          required
          aria-invalid={Boolean(fieldErrors.name)}
          aria-describedby={fieldErrors.name ? 'register-name-error' : undefined}
          className={`${inputBase} ${fieldErrors.name ? inputInvalid : inputDefault}`}
          placeholder="John Doe"
        />
        {fieldErrors.name && (
          <p id="register-name-error" role="alert" className="mt-1 text-sm text-danger">
            {fieldErrors.name}
          </p>
        )}
      </div>
      <div>
        <label htmlFor="register-email" className="block text-sm font-medium text-ink-secondary mb-1">
          Email
        </label>
        <input
          ref={emailRef}
          id="register-email"
          name="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            clearFieldError('email');
          }}
          required
          aria-invalid={Boolean(fieldErrors.email)}
          aria-describedby={fieldErrors.email ? 'register-email-error' : undefined}
          className={`${inputBase} ${fieldErrors.email ? inputInvalid : inputDefault}`}
          placeholder="you@example.com"
        />
        {fieldErrors.email && (
          <p id="register-email-error" role="alert" className="mt-1 text-sm text-danger">
            {fieldErrors.email}
          </p>
        )}
      </div>
      <div>
        <label htmlFor="register-password" className="block text-sm font-medium text-ink-secondary mb-1">
          Password
        </label>
        <input
          ref={passwordRef}
          id="register-password"
          name="password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value);
            clearFieldError('password');
          }}
          required
          minLength={PASSWORD_MIN_LENGTH}
          aria-invalid={Boolean(fieldErrors.password)}
          aria-describedby={fieldErrors.password ? 'register-password-error' : undefined}
          className={`${inputBase} ${fieldErrors.password ? inputInvalid : inputDefault}`}
          placeholder="At least 8 characters"
        />
        {fieldErrors.password && (
          <p id="register-password-error" role="alert" className="mt-1 text-sm text-danger">
            {fieldErrors.password}
          </p>
        )}
      </div>
      <p
        id="register-error"
        role="alert"
        aria-live="polite"
        className="min-h-[1.25rem] text-sm text-danger"
      >
        {formError}
      </p>
      <button
        type="submit"
        disabled={isSubmitting}
        className="w-full rounded-md bg-accent px-4 py-2.5 font-medium text-accent-contrast transition-colors hover:bg-accent-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-accent/40 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isSubmitting ? 'Creating account...' : 'Register'}
      </button>
      <p className="text-center text-sm text-ink-secondary">
        Already have an account?{' '}
        <button
          type="button"
          onClick={onSwitchToLogin}
          className="font-medium text-accent hover:text-accent-hover focus:outline-none focus-visible:underline"
        >
          Sign in
        </button>
      </p>
    </form>
  );
}
