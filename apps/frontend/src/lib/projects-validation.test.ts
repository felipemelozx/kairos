import {
  PROJECT_COLOR_PATTERN,
  PROJECT_COLOR_PRESETS,
  PROJECT_DESCRIPTION_MAX_LENGTH,
  PROJECT_NAME_MAX_LENGTH,
  getFirstInvalidField,
  hasErrors,
  pickFieldErrors,
  validateProject,
} from './projects-validation';

describe('projects-validation', () => {
  it('should expose DTO bounds', () => {
    expect(PROJECT_NAME_MAX_LENGTH).toBe(100);
    expect(PROJECT_DESCRIPTION_MAX_LENGTH).toBe(500);
  });

  it('should expose the backend hex pattern', () => {
    expect(PROJECT_COLOR_PATTERN.source).toBe('^#[0-9A-Fa-f]{6}$');
  });

  it('should reuse DESIGN.md token hexes as presets', () => {
    expect(PROJECT_COLOR_PRESETS).toEqual([
      '#0F766E',
      '#1D4ED8',
      '#15803D',
      '#B45309',
      '#B91C1C',
      '#0B0B0C',
    ]);
  });

  describe('validateProject', () => {
    it('should reject a blank name', () => {
      const errors = validateProject({ name: '', description: '', color: '#0F766E' });

      expect(errors.name).toBe('Enter a project name.');
    });

    it('should reject a whitespace-only name', () => {
      const errors = validateProject({ name: '   ', description: '', color: '#0F766E' });

      expect(errors.name).toBe('Enter a project name.');
    });

    it('should accept a name of exactly 100 characters', () => {
      const name = 'a'.repeat(PROJECT_NAME_MAX_LENGTH);
      const errors = validateProject({ name, description: '', color: '#0F766E' });

      expect(errors.name).toBeUndefined();
    });

    it('should reject a name longer than 100 characters', () => {
      const name = 'a'.repeat(PROJECT_NAME_MAX_LENGTH + 1);
      const errors = validateProject({ name, description: '', color: '#0F766E' });

      expect(errors.name).toBe('Name must be 100 characters or fewer.');
    });

    it('should accept an absent description', () => {
      const errors = validateProject({ name: 'Deep Work', description: '', color: '#0F766E' });

      expect(errors.description).toBeUndefined();
    });

    it('should accept a description of exactly 500 characters', () => {
      const description = 'a'.repeat(PROJECT_DESCRIPTION_MAX_LENGTH);
      const errors = validateProject({ name: 'Deep Work', description, color: '#0F766E' });

      expect(errors.description).toBeUndefined();
    });

    it('should reject a description longer than 500 characters', () => {
      const description = 'a'.repeat(PROJECT_DESCRIPTION_MAX_LENGTH + 1);
      const errors = validateProject({ name: 'Deep Work', description, color: '#0F766E' });

      expect(errors.description).toBe('Description must be 500 characters or fewer.');
    });

    it('should reject a missing color on create', () => {
      const errors = validateProject({ name: 'Deep Work', description: '', color: '' });

      expect(errors.color).toBe('Choose a color.');
    });

    it('should reject a malformed color', () => {
      const errors = validateProject({ name: 'Deep Work', description: '', color: 'not-a-hex' });

      expect(errors.color).toBe('Enter a valid hex color (for example #0F766E).');
    });

    it('should accept a lowercase hex color', () => {
      const errors = validateProject({ name: 'Deep Work', description: '', color: '#0f766e' });

      expect(errors.color).toBeUndefined();
    });

    it('should accept an uppercase hex color', () => {
      const errors = validateProject({ name: 'Deep Work', description: '', color: '#0F766E' });

      expect(errors.color).toBeUndefined();
    });

    it('should not require a color when requireColor is false', () => {
      const errors = validateProject(
        { name: 'Deep Work', description: '', color: '' },
        { requireColor: false }
      );

      expect(errors.color).toBeUndefined();
    });

    it('should still reject a malformed color when requireColor is false', () => {
      const errors = validateProject(
        { name: 'Deep Work', description: '', color: '#12' },
        { requireColor: false }
      );

      expect(errors.color).toBe('Enter a valid hex color (for example #0F766E).');
    });
  });

  describe('getFirstInvalidField', () => {
    it('should respect the provided field order', () => {
      const order = ['name', 'description', 'color'] as const;

      expect(
        getFirstInvalidField({ description: 'bad', color: 'bad' }, order)
      ).toBe('description');
      expect(getFirstInvalidField({ color: 'bad' }, order)).toBe('color');
      expect(getFirstInvalidField({}, order)).toBeNull();
    });
  });

  describe('pickFieldErrors', () => {
    it('should return only known fields', () => {
      const errors = pickFieldErrors(
        { name: 'Name taken', unknown: 'ignored', color: 'Bad hex' },
        ['name', 'description', 'color'] as const
      );

      expect(errors).toEqual({ name: 'Name taken', color: 'Bad hex' });
    });

    it('should return an empty object when details are undefined', () => {
      expect(pickFieldErrors(undefined, ['name'] as const)).toEqual({});
    });
  });

  describe('hasErrors', () => {
    it('should detect a present message', () => {
      expect(hasErrors({ name: 'bad' })).toBe(true);
      expect(hasErrors({ name: undefined })).toBe(false);
      expect(hasErrors({})).toBe(false);
    });
  });
});
