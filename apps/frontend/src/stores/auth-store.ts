import { create } from 'zustand';
import { authApi, LoginData, RegisterData, User } from '@/lib/auth-api';

interface AuthState {
  user: User | null;
  isLoading: boolean;
  isSubmitting: boolean;
  login: (data: LoginData) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  fetchMe: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isLoading: false,
  isSubmitting: false,

  login: async (data: LoginData) => {
    set({ isSubmitting: true });
    try {
      const user = await authApi.login(data);
      set({ user, isSubmitting: false });
    } catch (error) {
      set({ isSubmitting: false });
      throw error;
    }
  },

  register: async (data: RegisterData) => {
    set({ isSubmitting: true });
    try {
      const user = await authApi.register(data);
      set({ user, isSubmitting: false });
    } catch (error) {
      set({ isSubmitting: false });
      throw error;
    }
  },

  logout: async () => {
    await authApi.logout();
    set({ user: null });
  },

  fetchMe: async () => {
    set({ isLoading: true });
    try {
      const user = await authApi.me();
      set({ user, isLoading: false });
    } catch (error) {
      set({ user: null, isLoading: false });
    }
  },
}));