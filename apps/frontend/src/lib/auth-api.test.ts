import { authApi } from './auth-api';
import { apiFetch } from './api';

jest.mock('./api');

const mockApiFetch = apiFetch as jest.MockedFunction<typeof apiFetch>;

describe('authApi', () => {
  const mockUser = {
    id: '550e8400-e29b-41d4-a716-446655440000',
    email: 'test@example.com',
    name: 'Test User',
    avatarUrl: null,
    provider: 'LOCAL',
    active: true,
    createdAt: '2026-09-09T10:00:00Z',
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('register', () => {
    it('should POST register endpoint and return user', async () => {
      mockApiFetch.mockResolvedValue({ data: mockUser } as never);

      const result = await authApi.register({
        email: 'test@example.com',
        password: 'password123',
        name: 'Test User',
      });

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/auth/register',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({
            email: 'test@example.com',
            password: 'password123',
            name: 'Test User',
          }),
        })
      );
      expect(result).toEqual(mockUser);
    });
  });

  describe('login', () => {
    it('should POST login endpoint and return user', async () => {
      mockApiFetch.mockResolvedValue({ data: mockUser } as never);

      const result = await authApi.login({
        email: 'test@example.com',
        password: 'password123',
      });

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/auth/login',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({
            email: 'test@example.com',
            password: 'password123',
          }),
        })
      );
      expect(result).toEqual(mockUser);
    });
  });

  describe('logout', () => {
    it('should POST logout endpoint', async () => {
      mockApiFetch.mockResolvedValue(undefined as never);

      await authApi.logout();

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/auth/logout',
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  describe('refresh', () => {
    it('should POST refresh endpoint', async () => {
      mockApiFetch.mockResolvedValue(undefined as never);

      await authApi.refresh();

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/auth/refresh',
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  describe('me', () => {
    it('should GET me endpoint and return user', async () => {
      mockApiFetch.mockResolvedValue({ data: mockUser } as never);

      const result = await authApi.me();

      expect(mockApiFetch).toHaveBeenCalledWith('/api/auth/me');
      expect(result).toEqual(mockUser);
    });
  });
});