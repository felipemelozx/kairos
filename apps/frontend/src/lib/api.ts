import { getCsrfToken } from './csrf';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

// Endpoints that must never trigger an automatic refresh (they either
// establish the session or are the refresh itself - retrying them would loop).
const NO_REFRESH_ENDPOINTS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly details?: Record<string, string>;

  constructor(status: number, message: string, code?: string, details?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
    details?: Record<string, string>;
  };
}

function buildHeaders(options: RequestInit): Headers {
  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers);

  if (!SAFE_METHODS.has(method)) {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers.set('X-CSRF-Token', csrfToken);
    }
  }

  return headers;
}

async function tryRefresh(): Promise<boolean> {
  try {
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    });
    return response.ok;
  } catch {
    return false;
  }
}

function toApiError(status: number, body: ApiErrorBody | undefined): ApiError {
  const message = body?.error?.message || `API error: ${status}`;
  return new ApiError(status, message, body?.error?.code, body?.error?.details);
}

async function parseErrorBody(response: Response): Promise<ApiErrorBody | undefined> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return undefined;
  }
}

export async function apiFetch<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const doFetch = async (): Promise<Response> =>
    fetch(url, {
      ...options,
      headers: buildHeaders(options),
      credentials: 'include',
    });

  let response = await doFetch();

  // Access token expired but session may still be valid: try a single
  // silent refresh and retry the original request once.
  if (
    response.status === 401 &&
    !NO_REFRESH_ENDPOINTS.some((endpoint) => url.startsWith(endpoint))
  ) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      response = await doFetch();
    }
  }

  if (!response.ok) {
    throw toApiError(response.status, await parseErrorBody(response));
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return response.json() as Promise<T>;
  }

  return undefined as T;
}
