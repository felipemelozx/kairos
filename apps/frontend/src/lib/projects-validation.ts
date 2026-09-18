export {
  getFirstInvalidField,
  hasErrors,
  pickFieldErrors,
} from './auth-validation';

export const PROJECT_NAME_MAX_LENGTH = 100;
export const PROJECT_DESCRIPTION_MAX_LENGTH = 500;
export const PROJECT_COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/;

export const PROJECT_COLOR_PRESETS = [
  '#0F766E',
  '#1D4ED8',
  '#15803D',
  '#B45309',
  '#B91C1C',
  '#0B0B0C',
] as const;

export interface ProjectFields {
  name: string | null | undefined;
  description: string | null | undefined;
  color: string | null | undefined;
}

export type ProjectField = keyof ProjectFields;

export type ProjectErrors = Partial<Record<ProjectField, string>>;

export interface ValidateProjectOptions {
  requireColor?: boolean;
}

export function validateProject(
  fields: ProjectFields,
  options: ValidateProjectOptions = {}
): ProjectErrors {
  const { requireColor = true } = options;
  const errors: ProjectErrors = {};
  const name = (fields.name ?? '').trim();
  const description = (fields.description ?? '').trim();
  const color = (fields.color ?? '').trim();

  if (!name) {
    errors.name = 'Enter a project name.';
  } else if (name.length > PROJECT_NAME_MAX_LENGTH) {
    errors.name = `Name must be ${PROJECT_NAME_MAX_LENGTH} characters or fewer.`;
  }

  if (description.length > PROJECT_DESCRIPTION_MAX_LENGTH) {
    errors.description = `Description must be ${PROJECT_DESCRIPTION_MAX_LENGTH} characters or fewer.`;
  }

  if (!color) {
    if (requireColor) {
      errors.color = 'Choose a color.';
    }
  } else if (!PROJECT_COLOR_PATTERN.test(color)) {
    errors.color = 'Enter a valid hex color (for example #0F766E).';
  }

  return errors;
}
