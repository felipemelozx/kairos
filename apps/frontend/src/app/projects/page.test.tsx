import { render, screen, waitFor } from '@testing-library/react';
import ProjectsPage from './page';
import { useAuthStore } from '@/stores/auth-store';
import { useProjectsStore } from '@/stores/projects-store';

jest.mock('@/stores/auth-store');
jest.mock('@/stores/projects-store');

const mockUseAuthStore = useAuthStore as unknown as jest.Mock;
const mockUseProjectsStore = useProjectsStore as unknown as jest.Mock;

const mockReplace = jest.fn();
const mockPush = jest.fn();
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
}));

describe('ProjectsPage', () => {
  const fetchProjects = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    fetchProjects.mockResolvedValue(undefined);
    mockUseProjectsStore.mockReturnValue({
      projects: [],
      isLoading: false,
      isSubmitting: false,
      error: null,
      fetchProjects,
      createProject: jest.fn(),
      updateProject: jest.fn(),
      deleteProject: jest.fn(),
    });
  });

  it('should render the protected screen and fetch projects when authenticated', async () => {
    mockUseAuthStore.mockReturnValue({ user: { id: '1' }, status: 'authenticated', isLoading: false });

    render(<ProjectsPage />);

    expect(screen.getByRole('heading', { name: 'Projects' })).toBeInTheDocument();
    await waitFor(() => {
      expect(fetchProjects).toHaveBeenCalledTimes(1);
    });
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('should redirect to / and not fetch when unauthenticated', async () => {
    mockUseAuthStore.mockReturnValue({ user: null, status: 'anonymous', isLoading: false });

    render(<ProjectsPage />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/');
    });
    expect(screen.queryByRole('heading', { name: 'Projects' })).not.toBeInTheDocument();
  });

  it('should not fetch while auth is still loading', () => {
    mockUseAuthStore.mockReturnValue({ user: null, status: 'checking', isLoading: true });

    render(<ProjectsPage />);

    expect(screen.getByRole('status')).toHaveTextContent('Loading...');
  });
});
