import { getCsrfToken } from '../lib/csrf';

describe('getCsrfToken', () => {
  beforeEach(() => {
    document.cookie = 'CSRF_TOKEN=; max-age=0';
    document.cookie = 'OTHER=value; path=/';
  });

  it('should return token from cookie', () => {
    document.cookie = 'CSRF_TOKEN=abc123; path=/';

    const token = getCsrfToken();

    expect(token).toBe('abc123');
  });

  it('should return null when cookie missing', () => {
    const token = getCsrfToken();

    expect(token).toBeNull();
  });

  it('should handle encoded values', () => {
    document.cookie = 'CSRF_TOKEN=abc%2F123%3D; path=/';

    const token = getCsrfToken();

    expect(token).toBe('abc/123=');
  });

  it('should return null when other cookies present but not CSRF_TOKEN', () => {
    document.cookie = 'OTHER=value; path=/';

    const token = getCsrfToken();

    expect(token).toBeNull();
  });
});
