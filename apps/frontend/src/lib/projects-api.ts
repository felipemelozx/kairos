import { apiFetch } from './api';

export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';

export interface Project {
  id: string;
  userId: string;
  name: string;
  description: string | null;
  color: string;
  status: ProjectStatus;
  createdAt: string;
}

export interface CreateProjectData {
  name: string;
  description?: string;
  color: string;
}

export interface UpdateProjectData {
  name?: string;
  description?: string;
  color?: string;
  status?: ProjectStatus;
}

interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  timestamp: string;
}

function ensureData<T>(data: T | null, fallbackMessage: string): T {
  if (data === null || data === undefined) {
    throw new Error(fallbackMessage);
  }
  return data;
}

export const projectsApi = {
  list: async (): Promise<Project[]> => {
    const response = await apiFetch<ApiEnvelope<Project[]>>('/api/projects');
    return response.data ?? [];
  },

  create: async (data: CreateProjectData): Promise<Project> => {
    const response = await apiFetch<ApiEnvelope<Project>>('/api/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return ensureData(response.data, 'Could not create the project. Please try again.');
  },

  update: async (id: string, data: UpdateProjectData): Promise<Project> => {
    const response = await apiFetch<ApiEnvelope<Project>>(`/api/projects/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return ensureData(response.data, 'Could not update the project. Please try again.');
  },

  remove: async (id: string): Promise<void> => {
    await apiFetch(`/api/projects/${id}`, { method: 'DELETE' });
  },
};
