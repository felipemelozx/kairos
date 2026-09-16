import { create } from 'zustand';
import {
  CreateProjectData,
  Project,
  UpdateProjectData,
  projectsApi,
} from '@/lib/projects-api';

interface ProjectsState {
  projects: Project[];
  isLoading: boolean;
  isSubmitting: boolean;
  error: string | null;
  fetchProjects: () => Promise<void>;
  createProject: (data: CreateProjectData) => Promise<Project>;
  updateProject: (id: string, data: UpdateProjectData) => Promise<Project>;
  deleteProject: (id: string) => Promise<void>;
}

const LOAD_ERROR = 'Could not load projects. Please try again.';

export const useProjectsStore = create<ProjectsState>((set) => ({
  projects: [],
  isLoading: false,
  isSubmitting: false,
  error: null,

  fetchProjects: async () => {
    set({ isLoading: true, error: null });
    try {
      const projects = await projectsApi.list();
      set({ projects, isLoading: false });
    } catch (error) {
      set({
        isLoading: false,
        error: error instanceof Error ? error.message : LOAD_ERROR,
      });
    }
  },

  createProject: async (data: CreateProjectData) => {
    set({ isSubmitting: true });
    try {
      const project = await projectsApi.create(data);
      set((state) => ({ projects: [...state.projects, project], isSubmitting: false }));
      return project;
    } catch (error) {
      set({ isSubmitting: false });
      throw error;
    }
  },

  updateProject: async (id: string, data: UpdateProjectData) => {
    set({ isSubmitting: true });
    try {
      const project = await projectsApi.update(id, data);
      set((state) => ({
        projects: state.projects.map((item) => (item.id === id ? project : item)),
        isSubmitting: false,
      }));
      return project;
    } catch (error) {
      set({ isSubmitting: false });
      throw error;
    }
  },

  deleteProject: async (id: string) => {
    set({ isSubmitting: true });
    try {
      await projectsApi.remove(id);
      set((state) => ({
        projects: state.projects.filter((item) => item.id !== id),
        isSubmitting: false,
      }));
    } catch (error) {
      set({ isSubmitting: false });
      throw error;
    }
  },
}));
