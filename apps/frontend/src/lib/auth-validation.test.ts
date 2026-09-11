import {
  EMAIL_PATTERN,
  NAME_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  getFirstInvalidField,
  hasErrors,
  pickFieldErrors,
  validateLogin,
  validateRegister,
} from './auth-validation';

describe('auth-validation constants', () => {
  it('exposes the derived backend rules', () => {
    expect(PASSWORD_MIN_LENGTH).toBe(8);
    expect(NAME_MAX_LENGTH).toBe(100);
    expect(EMAIL_PATTERN.test('user@example.com')).toBe(true);
    expect(EMAIL_PATTERN.test('not-an-email')).toBe(false);
  });
});

describe('validateLogin', () => {
  it('requires the email field', () => {
    expect(validateLogin({ email: '', password: 'secret' })).toEqual({
      email: 'Enter your email address.',
    });
  });

  it('treats a whitespace-only email as empty', () => {
    expect(validateLogin({ email: '   ', password: 'secret' })).toEqual({
      email: 'Enter your email address.',
    });
  });

  it('rejects a malformed email', () => {
    expect(validateLogin({ email: 'not-an-email', password: 'secret' })).toEqual({
      email: 'Enter a valid email address.',
    });
  });

  it('requires the password field', () => {
    expect(validateLogin({ email: 'user@example.com', password: '' })).toEqual({
      password: 'Enter your password.',
    });
  });

  it('reports every invalid field at once', () => {
    expect(validateLogin({ email: '', password: '' })).toEqual({
      email: 'Enter your email address.',
      password: 'Enter your password.',
    });
  });

  it('accepts valid credentials and does not enforce a password minimum on login', () => {
    expect(validateLogin({ email: 'user@example.com', password: 'x' })).toEqual({});
  });
});

describe('validateRegister', () => {
  const valid = { name: 'John Doe', email: 'john@example.com', password: 'password123' };

  it('requires the name field', () => {
    expect(validateRegister({ ...valid, name: '   ' })).toEqual({
      name: 'Enter your name.',
    });
  });

  it('rejects a name longer than the backend limit', () => {
    expect(validateRegister({ ...valid, name: 'a'.repeat(NAME_MAX_LENGTH + 1) })).toEqual({
      name: `Name must be ${NAME_MAX_LENGTH} characters or fewer.`,
    });
  });

  it('accepts a name at the backend limit', () => {
    expect(validateRegister({ ...valid, name: 'a'.repeat(NAME_MAX_LENGTH) })).toEqual({});
  });

  it('requires the email field', () => {
    expect(validateRegister({ ...valid, email: '' })).toEqual({
      email: 'Enter your email address.',
    });
  });

  it('rejects a malformed email', () => {
    expect(validateRegister({ ...valid, email: 'john@' })).toEqual({
      email: 'Enter a valid email address.',
    });
  });

  it('requires the password field', () => {
    expect(validateRegister({ ...valid, password: '' })).toEqual({
      password: 'Enter a password.',
    });
  });

  it('rejects a password shorter than the backend minimum', () => {
    expect(validateRegister({ ...valid, password: 'short' })).toEqual({
      password: `Password must be at least ${PASSWORD_MIN_LENGTH} characters.`,
    });
  });

  it('reports every invalid field at once', () => {
    expect(validateRegister({ name: '', email: '', password: '' })).toEqual({
      name: 'Enter your name.',
      email: 'Enter your email address.',
      password: 'Enter a password.',
    });
  });

  it('accepts valid registration data', () => {
    expect(validateRegister(valid)).toEqual({});
  });
});

describe('getFirstInvalidField', () => {
  it('returns the first invalid field in visual order', () => {
    const errors = { email: 'bad', password: 'missing' };
    expect(getFirstInvalidField(errors, ['email', 'password'])).toBe('email');
  });

  it('skips valid fields and returns the next invalid one', () => {
    const errors = { password: 'missing' };
    expect(getFirstInvalidField(errors, ['email', 'password'])).toBe('password');
  });

  it('returns null when there are no errors', () => {
    expect(getFirstInvalidField({}, ['email', 'password'])).toBeNull();
  });
});

describe('pickFieldErrors', () => {
  it('keeps only known fields with messages', () => {
    const details = { email: 'Invalid email format', unknown: 'ignored' };
    expect(pickFieldErrors(details, ['email', 'password'])).toEqual({
      email: 'Invalid email format',
    });
  });

  it('ignores empty messages', () => {
    expect(pickFieldErrors({ email: '' }, ['email'])).toEqual({});
  });

  it('returns an empty object when details are undefined', () => {
    expect(pickFieldErrors(undefined, ['email'])).toEqual({});
  });
});

describe('hasErrors', () => {
  it('is true when any field has a message', () => {
    expect(hasErrors({ email: 'bad', password: undefined })).toBe(true);
  });

  it('is false when all fields are empty', () => {
    expect(hasErrors({ email: undefined, password: undefined })).toBe(false);
  });
});
