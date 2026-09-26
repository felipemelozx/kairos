const CSRF_COOKIE_NAME = 'CSRF_TOKEN';

export function getCsrfToken(): string | null {
  if (typeof document === 'undefined') {
    return null;
  }

  const cookies = document.cookie.split(';');
  for (const cookie of cookies) {
    const idx = cookie.indexOf('=');
    if (idx < 0) {
      continue;
    }
    const name = cookie.slice(0, idx).trim();
    if (name === CSRF_COOKIE_NAME) {
      const value = cookie.slice(idx + 1).trim();
      try {
        return decodeURIComponent(value);
      } catch {
        // Malformed cookie (extensions, stale values): let the API return
        // a controlled CSRF error instead of crashing the request.
        return value || null;
      }
    }
  }

  return null;
}
