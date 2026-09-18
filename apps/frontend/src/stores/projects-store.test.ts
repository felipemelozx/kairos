import { projectsApi, Project } from '@/lib/projects-api';

jest.mock('@/lib/projects-api');

const mockProjectsApi = projectsApi as jest.Mocked<typeof projectsApi>;

import { useProjectsStore } from './projects-store';

const mockProject: Project = {
  id: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  name: 'Deep Work',
  description: 'Focused work',
  color: '#0F766E',
  status: 'ACTIVE',
  createdAt: '2026-09-12T10:00:00Z',
};

const mockArchived: Project = {
  ...mockProject,
  id: '33333333-3333-3333-3333-333333333333',
  name: 'Reading',
  status: 'ARCHIVED',
};

describe('projectsStore', () => {
  beforeEach(() => {
    useProjectsStore.setState({
      projects: [],
      isLoading: false,
      isSubmitting: false,
      error: null,
    });
    jest.clearAllMocks();
  });

  describe('fetchProjects', () => {
    it('should set projects and clear error', async () => {
      useProjectsStore.setState({ error: 'stale error' });
      mockProjectsApi.list.mockResolvedValue([mockProject, mockArchived]);

      await useProjectsStore.getState().fetchProjects();

      expect(useProjectsStore.getState().projects).toEqual([mockProject, mockArchived]);
      expect(useProjectsStore.getState().error).toBeNull();
      expect(useProjectsStore.getState().isLoading).toBe(false);
    });

    it('should set error on failure and keep the list intact', async () => {
      useProjectsStore.setState({ projects: [mockProject] });
      mockProjectsApi.list.mockRejectedValue(new Error('Network error'));

      await useProjectsStore.getState().fetchProjects();

      expect(useProjectsStore.getState().error).toBe('Network error');
      expect(useProjectsStore.getState().projects).toEqual([mockProject]);
      expect(useProjectsStore.getState().isLoading).toBe(false);
    });
  });

  describe('createProject', () => {
    it('should append the created project and toggle isSubmitting', async () => {
      mockProjectsApi.create.mockResolvedValue(mockProject);

      const result = await useProjectsStore
        .getState()
        .createProject({ name: 'Deep Work', color: '#0F766E' });

      expect(mockProjectsApi.create).toHaveBeenCalledWith({
        name: 'Deep Work',
        color: '#0F766E',
      });
      expect(result).toEqual(mockProject);
      expect(useProjectsStore.getState().projects).toEqual([mockProject]);
      expect(useProjectsStore.getState().isSubmitting).toBe(false);
    });

    it('should rethrow and reset isSubmitting on failure', async () => {
      mockProjectsApi.create.mockRejectedValue(new Error('Create failed'));

      await expect(
        useProjectsStore.getState().createProject({ name: 'Deep Work', color: '#0F766E' })
      ).rejects.toThrow('Create failed');

      expect(useProjectsStore.getState().projects).toEqual([]);
      expect(useProjectsStore.getState().isSubmitting).toBe(false);
    });
  });

  describe('updateProject', () => {
    it('should replace the matching project', async () => {
      useProjectsStore.setState({ projects: [mockProject, mockArchived] });
      const updated = { ...mockProject, status: 'ARCHIVED' as const };
      mockProjectsApi.update.mockResolvedValue(updated);

      await useProjectsStore.getState().updateProject(mockProject.id, { status: 'ARCHIVED' });

      expect(mockProjectsApi.update).toHaveBeenCalledWith(mockProject.id, {
        status: 'ARCHIVED',
      });
      expect(useProjectsStore.getState().projects).toEqual([updated, mockArchived]);
      expect(useProjectsStore.getState().isSubmitting).toBe(false);
    });

    it('should rethrow and keep state on failure', async () => {
      useProjectsStore.setState({ projects: [mockProject] });
      mockProjectsApi.update.mockRejectedValue(new Error('Update failed'));

      await expect(
        useProjectsStore.getState().updateProject(mockProject.id, { status: 'ARCHIVED' })
      ).rejects.toThrow('Update failed');

      expect(useProjectsStore.getState().projects).toEqual([mockProject]);
      expect(useProjectsStore.getState().isSubmitting).toBe(false);
    });
  });

  describe('deleteProject', () => {
    it('should remove the matching project', async () => {
      useProjectsStore.setState({ projects: [mockProject, mockArchived] });
      mockProjectsApi.remove.mockResolvedValue(undefined);

      await useProjectsStore.getState().deleteProject(mockProject.id);

      expect(mockProjectsApi.remove).toHaveBeenCalledWith(mockProject.id);
      expect(useProjectsStore.getState().projects).toEqual([mockArchived]);
      expect(useProjectsStore.getState().isSubmitting).toBe(false);
    });

    it('should rethrow and keep state on failure', async () => {
      useProjectsStore.setState({ projects: [mockProject] });
      mockProjectsApi.remove.mockRejectedValue(new Error('Delete failed'));

      await expect(
        useProjectsStore.getState().deleteProject(mockProject.id)
      ).rejects.toThrow('Delete failed');

      expect(useProjectsStore.getState().projects).toEqual([mockProject]);
      expect(useProjectsStore.getState().isSubmitting).toBe(false);
    });
  });
});
