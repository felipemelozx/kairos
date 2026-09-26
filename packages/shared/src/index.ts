// @kairos/shared — single source of truth for cross-app contracts (FIX-003).
// Frontend and backend DTOs must stay compatible with these shapes.

export interface SharedUser {
  id: string;
  email: string;
  name: string;
}

export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';

export interface SharedProject {
  id: string;
  userId: string;
  name: string;
  description: string | null;
  color: string;
  status: ProjectStatus;
  createdAt: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  error?: { code: string; message: string; details?: Record<string, string> | null };
}

export function isApiEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  if (typeof value !== 'object' || value === null) return false;
  return typeof (value as ApiEnvelope<T>).success === 'boolean';
}
