import { apiFetch } from '../lib/api';
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
      credentials: 'same-origin',
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

  it('should use same-origin credentials', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve({}),
    } as Response);

    await apiFetch('/api/test');

    expect(mockFetch).toHaveBeenCalledWith('/api/test', expect.objectContaining({
      credentials: 'same-origin',
    }));
  });
});
