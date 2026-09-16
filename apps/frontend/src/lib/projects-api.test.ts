import { projectsApi } from './projects-api';
import { apiFetch } from './api';

jest.mock('./api');

const mockApiFetch = apiFetch as jest.MockedFunction<typeof apiFetch>;

const mockProject = {
  id: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  name: 'Deep Work',
  description: 'Focused work',
  color: '#0F766E',
  status: 'ACTIVE' as const,
  createdAt: '2026-09-12T10:00:00Z',
};

describe('projectsApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('list', () => {
    it('should GET /api/projects and unwrap data to Project[]', async () => {
      mockApiFetch.mockResolvedValue({ data: [mockProject] } as never);

      const result = await projectsApi.list();

      expect(mockApiFetch).toHaveBeenCalledWith('/api/projects');
      expect(result).toEqual([mockProject]);
    });
  });

  describe('create', () => {
    it('should POST /api/projects with the payload and return the project', async () => {
      mockApiFetch.mockResolvedValue({ data: mockProject } as never);

      const result = await projectsApi.create({
        name: 'Deep Work',
        description: 'Focused work',
        color: '#0F766E',
      });

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/projects',
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: 'Deep Work',
            description: 'Focused work',
            color: '#0F766E',
          }),
        })
      );
      expect(result).toEqual(mockProject);
    });
  });

  describe('update', () => {
    it('should PATCH /api/projects/{id} with only provided fields', async () => {
      mockApiFetch.mockResolvedValue({ data: mockProject } as never);

      const result = await projectsApi.update('abc-123', { status: 'ARCHIVED' });

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/projects/abc-123',
        expect.objectContaining({
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: 'ARCHIVED' }),
        })
      );
      expect(result).toEqual(mockProject);
    });
  });

  describe('remove', () => {
    it('should DELETE /api/projects/{id}', async () => {
      mockApiFetch.mockResolvedValue(undefined as never);

      await projectsApi.remove('abc-123');

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/projects/abc-123',
        expect.objectContaining({ method: 'DELETE' })
      );
    });
  });
});
