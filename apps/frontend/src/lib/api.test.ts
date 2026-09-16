import { apiFetch, ApiError } from '../lib/api';
import { getCsrfToken } from '../lib/csrf';

jest.mock('../lib/csrf');

const mockGetCsrfToken = getCsrfToken as jest.MockedFunction<typeof getCsrfToken>;

describe('apiFetch', () => {
  let mockFetch: jest.MockedFunction<typeof fetch>;

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetch = jest.fn() as jest.MockedFunction<typeof fetch>;
    global.fetch = mockFetch;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should inject CSRF header on POST', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-token-123');
    mockFetch.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({ success: true }),
    } as Response);

    await apiFetch('/api/test', { method: 'POST' });

    expect(mockFetch).toHaveBeenCalledWith('/api/test', expect.objectContaining({
      method: 'POST',
      headers: expect.any(Headers),
      credentials: 'include',
    }));

    const callHeaders = mockFetch.mock.calls[0][1]?.headers as Headers;
    expect(callHeaders.get('X-CSRF-Token')).toBe('csrf-token-123');
  });

  it('should inject CSRF header on PUT', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-token-456');
    mockFetch.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({}),
    } as Response);

    await apiFetch('/api/test', { method: 'PUT' });

    const callHeaders = mockFetch.mock.calls[0][1]?.headers as Headers;
    expect(callHeaders.get('X-CSRF-Token')).toBe('csrf-token-456');
  });

  it('should inject CSRF header on DELETE', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-token-789');
    mockFetch.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({}),
    } as Response);

    await apiFetch('/api/test', { method: 'DELETE' });

    const callHeaders = mockFetch.mock.calls[0][1]?.headers as Headers;
    expect(callHeaders.get('X-CSRF-Token')).toBe('csrf-token-789');
  });

  it('should not inject CSRF header on GET', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-token-123');
    mockFetch.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({}),
    } as Response);

    await apiFetch('/api/test');

    const callHeaders = mockFetch.mock.calls[0][1]?.headers as Headers;
    expect(callHeaders.get('X-CSRF-Token')).toBeNull();
  });

  it('should not inject CSRF header on OPTIONS', async () => {
    mockGetCsrfToken.mockReturnValue('csrf-token-123');
    mockFetch.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({}),
    } as Response);

    await apiFetch('/api/test', { method: 'OPTIONS' });

    const callHeaders = mockFetch.mock.calls[0][1]?.headers as Headers;
    expect(callHeaders.get('X-CSRF-Token')).toBeNull();
  });

  it('should not inject CSRF header when token is null', async () => {
    mockGetCsrfToken.mockReturnValue(null);
    mockFetch.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({}),
    } as Response);

    await apiFetch('/api/test', { method: 'POST' });

    const callHeaders = mockFetch.mock.calls[0][1]?.headers as Headers;
    expect(callHeaders.get('X-CSRF-Token')).toBeNull();
  });

  it('should throw error on non-ok response', async () => {
    mockGetCsrfToken.mockReturnValue('token');
    mockFetch.mockResolvedValue({
      ok: false,
      status: 403,
      headers: new Headers(),
    } as Response);

    await expect(apiFetch('/api/test', { method: 'POST' })).rejects.toThrow('API error: 403');
  });

  it('should extract error message from response body', async () => {
    mockGetCsrfToken.mockReturnValue('token');
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () =>
        Promise.resolve({
          success: false,
          error: { code: 'UNAUTHORIZED', message: 'Invalid email or password' },
        }),
    } as Response);

    const error = (await apiFetch('/api/auth/login', { method: 'POST' }).catch((e) => e)) as ApiError;
    expect(error).toBeInstanceOf(Error);
    expect(error.message).toBe('Invalid email or password');
    expect(error.status).toBe(401);
    expect(error.code).toBe('UNAUTHORIZED');
  });

  it('should expose ApiError type guard fields', async () => {
    mockGetCsrfToken.mockReturnValue('token');
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () =>
        Promise.resolve({
          success: false,
          error: {
            code: 'VALIDATION_ERROR',
            message: 'Request validation failed',
            details: { email: 'Invalid email format' },
          },
        }),
    } as Response);

    const error = (await apiFetch('/api/auth/register', { method: 'POST' }).catch((e) => e)) as ApiError;
    expect(error.details).toEqual({ email: 'Invalid email format' });
  });

  it('should use include credentials', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({}),
    } as Response);

    await apiFetch('/api/test');

    expect(mockFetch).toHaveBeenCalledWith('/api/test', expect.objectContaining({
      credentials: 'include',
    }));
  });

  describe('silent refresh on 401', () => {
    const okJson = (payload: unknown) =>
      ({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: () => Promise.resolve(payload),
      }) as Response;

    const unauthorized = () =>
      ({
        ok: false,
        status: 401,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: () =>
          Promise.resolve({
            success: false,
            error: { code: 'UNAUTHORIZED', message: 'Not authenticated' },
          }),
      }) as Response;

    it('should refresh and retry the original request once', async () => {
      mockGetCsrfToken.mockReturnValue('token');
      mockFetch
        .mockResolvedValueOnce(unauthorized())
        .mockResolvedValueOnce(okJson({ success: true }))
        .mockResolvedValueOnce(okJson({ success: true, data: 'retried' }));

      const result = await apiFetch<{ data: string }>('/api/projects');

      expect(result).toEqual({ success: true, data: 'retried' });
      expect(mockFetch).toHaveBeenCalledTimes(3);
      expect(mockFetch.mock.calls[1][0]).toBe('/api/auth/refresh');
      expect(mockFetch.mock.calls[1][1]).toMatchObject({
        method: 'POST',
        credentials: 'include',
      });
      expect(mockFetch.mock.calls[2][0]).toBe('/api/projects');
    });

    it('should throw the 401 error when refresh fails', async () => {
      mockGetCsrfToken.mockReturnValue('token');
      mockFetch
        .mockResolvedValueOnce(unauthorized())
        .mockResolvedValueOnce(unauthorized());

      await expect(apiFetch('/api/projects')).rejects.toMatchObject({
        status: 401,
        code: 'UNAUTHORIZED',
      });
      expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('should not attempt refresh for login requests', async () => {
      mockGetCsrfToken.mockReturnValue('token');
      mockFetch.mockResolvedValueOnce(unauthorized());

      await expect(
        apiFetch('/api/auth/login', { method: 'POST' })
      ).rejects.toMatchObject({ status: 401 });
      expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('should not attempt refresh for refresh requests', async () => {
      mockFetch.mockResolvedValueOnce(unauthorized());

      await expect(
        apiFetch('/api/auth/refresh', { method: 'POST' })
      ).rejects.toMatchObject({ status: 401 });
      expect(mockFetch).toHaveBeenCalledTimes(1);
    });
  });
});
