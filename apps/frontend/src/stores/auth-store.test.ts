import { authApi, LoginData, RegisterData, User } from '@/lib/auth-api';

jest.mock('@/lib/auth-api');

const mockAuthApi = authApi as jest.Mocked<typeof authApi>;

const mockUser: User = {
  id: '550e8400-e29b-41d4-a716-446655440000',
  email: 'test@example.com',
  name: 'Test User',
  avatarUrl: null,
  provider: 'LOCAL',
  active: true,
  createdAt: '2026-09-09T10:00:00Z',
};

import { useAuthStore } from './auth-store';

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, isLoading: false });
    jest.clearAllMocks();
  });

  describe('login', () => {
    it('should set user after successful login', async () => {
      mockAuthApi.login.mockResolvedValue(mockUser);

      await useAuthStore.getState().login({ email: 'test@example.com', password: 'password123' });

      expect(useAuthStore.getState().user).toEqual(mockUser);
      expect(useAuthStore.getState().isLoading).toBe(false);
    });

    it('should throw error and keep user null on failed login', async () => {
      mockAuthApi.login.mockRejectedValue(new Error('Invalid credentials'));

      await expect(
        useAuthStore.getState().login({ email: 'test@example.com', password: 'wrong' })
      ).rejects.toThrow('Invalid credentials');

      expect(useAuthStore.getState().user).toBeNull();
      expect(useAuthStore.getState().isLoading).toBe(false);
    });
  });

  describe('register', () => {
    it('should set user after successful register', async () => {
      mockAuthApi.register.mockResolvedValue(mockUser);

      await useAuthStore
        .getState()
        .register({ email: 'test@example.com', password: 'password123', name: 'Test User' });

      expect(useAuthStore.getState().user).toEqual(mockUser);
    });

    it('should throw error on failed register', async () => {
      mockAuthApi.register.mockRejectedValue(new Error('Email already registered'));

      await expect(
        useAuthStore
          .getState()
          .register({ email: 'test@example.com', password: 'password123', name: 'Test' })
      ).rejects.toThrow('Email already registered');

      expect(useAuthStore.getState().user).toBeNull();
    });
  });

  describe('logout', () => {
    it('should clear user after logout', async () => {
      useAuthStore.setState({ user: mockUser });
      mockAuthApi.logout.mockResolvedValue(undefined);

      await useAuthStore.getState().logout();

      expect(useAuthStore.getState().user).toBeNull();
    });
  });

  describe('fetchMe', () => {
    it('should set user when authenticated', async () => {
      mockAuthApi.me.mockResolvedValue(mockUser);

      await useAuthStore.getState().fetchMe();

      expect(useAuthStore.getState().user).toEqual(mockUser);
      expect(useAuthStore.getState().isLoading).toBe(false);
    });

    it('should clear user when not authenticated', async () => {
      useAuthStore.setState({ user: mockUser });
      mockAuthApi.me.mockRejectedValue(new Error('Not authenticated'));

      await useAuthStore.getState().fetchMe();

      expect(useAuthStore.getState().user).toBeNull();
      expect(useAuthStore.getState().isLoading).toBe(false);
    });
  });
});