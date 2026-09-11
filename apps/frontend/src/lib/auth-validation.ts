export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const PASSWORD_MIN_LENGTH = 8;
export const NAME_MAX_LENGTH = 100;

export interface LoginFields {
  email: string;
  password: string;
}

export interface RegisterFields {
  name: string;
  email: string;
  password: string;
}

export type LoginField = keyof LoginFields;
export type RegisterField = keyof RegisterFields;

export type LoginErrors = Partial<Record<LoginField, string>>;
export type RegisterErrors = Partial<Record<RegisterField, string>>;

export function validateLogin(fields: LoginFields): LoginErrors {
  const errors: LoginErrors = {};
  const email = fields.email.trim();

  if (!email) {
    errors.email = 'Enter your email address.';
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Enter a valid email address.';
  }

  if (!fields.password) {
    errors.password = 'Enter your password.';
  }

  return errors;
}

export function validateRegister(fields: RegisterFields): RegisterErrors {
  const errors: RegisterErrors = {};
  const name = fields.name.trim();
  const email = fields.email.trim();

  if (!name) {
    errors.name = 'Enter your name.';
  } else if (name.length > NAME_MAX_LENGTH) {
    errors.name = `Name must be ${NAME_MAX_LENGTH} characters or fewer.`;
  }

  if (!email) {
    errors.email = 'Enter your email address.';
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Enter a valid email address.';
  }

  if (!fields.password) {
    errors.password = 'Enter a password.';
  } else if (fields.password.length < PASSWORD_MIN_LENGTH) {
    errors.password = `Password must be at least ${PASSWORD_MIN_LENGTH} characters.`;
  }

  return errors;
}

export function getFirstInvalidField<T extends string>(
  errors: Partial<Record<T, string>>,
  order: readonly T[]
): T | null {
  for (const field of order) {
    if (errors[field]) {
      return field;
    }
  }
  return null;
}

export function pickFieldErrors<T extends string>(
  details: Record<string, string> | undefined,
  fields: readonly T[]
): Partial<Record<T, string>> {
  const errors: Partial<Record<T, string>> = {};
  if (!details) {
    return errors;
  }
  for (const field of fields) {
    const message = details[field];
    if (message) {
      errors[field] = message;
    }
  }
  return errors;
}

export function hasErrors<T extends string>(
  errors: Partial<Record<T, string>>
): boolean {
  return Object.values(errors).some((message) => Boolean(message));
}
