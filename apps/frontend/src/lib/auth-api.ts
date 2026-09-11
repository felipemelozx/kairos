import { apiFetch } from './api';

export interface RegisterData {
  email: string;
  password: string;
  name: string;
}

export interface LoginData {
  email: string;
  password: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  avatarUrl: string | null;
  provider: 'LOCAL' | 'GOOGLE';
  active: boolean;
  createdAt: string;
}

interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  timestamp: string;
}

export const authApi = {
  register: async (data: RegisterData): Promise<User> => {
    const response = await apiFetch<ApiEnvelope<User>>('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return response.data as User;
  },

  login: async (data: LoginData): Promise<User> => {
    const response = await apiFetch<ApiEnvelope<User>>('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return response.data as User;
  },

  logout: async (): Promise<void> => {
    await apiFetch('/api/auth/logout', { method: 'POST' });
  },

  refresh: async (): Promise<void> => {
    await apiFetch('/api/auth/refresh', { method: 'POST' });
  },

  me: async (): Promise<User> => {
    const response = await apiFetch<ApiEnvelope<User>>('/api/auth/me');
    return response.data as User;
  },
};