import { create } from 'zustand';
import { authApi, LoginData, RegisterData, User } from '@/lib/auth-api';

export type AuthStatus = 'checking' | 'authenticated' | 'anonymous' | 'error';

interface AuthState {
  user: User | null;
  status: AuthStatus;
  /** @deprecated use status; kept for backwards compat with existing components */
  isLoading: boolean;
  isSubmitting: boolean;
  login: (data: LoginData) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  fetchMe: () => Promise<void>;
  markSessionExpired: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  status: 'checking',
  isLoading: true,
  isSubmitting: false,

  login: async (data: LoginData) => {
    set({ isSubmitting: true });
    try {
      const user = await authApi.login(data);
      set({ user, status: 'authenticated', isLoading: false, isSubmitting: false });
    } catch (error) {
      set({ isSubmitting: false });
      throw error;
    }
  },

  register: async (data: RegisterData) => {
    set({ isSubmitting: true });
    try {
      const user = await authApi.register(data);
      set({ user, status: 'authenticated', isLoading: false, isSubmitting: false });
    } catch (error) {
      set({ isSubmitting: false });
      throw error;
    }
  },

  logout: async () => {
    await authApi.logout();
    set({ user: null, status: 'anonymous', isLoading: false });
  },

  fetchMe: async () => {
    set({ isLoading: true, status: 'checking' });
    try {
      const user = await authApi.me();
      set({ user, status: 'authenticated', isLoading: false });
    } catch (error) {
      // FIX-003: distinguish terminal 401 from transient errors.
      const { ApiError } = await import('@/lib/api');
      if (error instanceof ApiError && error.status === 401) {
        set({ user: null, status: 'anonymous', isLoading: false });
      } else {
        set({ user: null, status: 'error', isLoading: false });
      }
    }
  },

  markSessionExpired: () => {
    set({ user: null, status: 'anonymous', isLoading: false });
  },
}));
