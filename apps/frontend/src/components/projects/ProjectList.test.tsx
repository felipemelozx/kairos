import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProjectList } from './ProjectList';
import { Project } from '@/lib/projects-api';
import { useProjectsStore } from '@/stores/projects-store';

jest.mock('@/stores/projects-store');

const mockUseProjectsStore = useProjectsStore as unknown as jest.Mock;

const activeProject: Project = {
  id: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  name: 'Deep Work',
  description: 'Focused work',
  color: '#0F766E',
  status: 'ACTIVE',
  createdAt: '2026-09-12T10:00:00Z',
};

const archivedProject: Project = {
  ...activeProject,
  id: '33333333-3333-3333-3333-333333333333',
  name: 'Reading',
  description: null,
  status: 'ARCHIVED',
};

describe('ProjectList', () => {
  const fetchProjects = jest.fn();
  const createProject = jest.fn();
  const updateProject = jest.fn();
  const deleteProject = jest.fn();

  const setStore = (overrides: Record<string, unknown> = {}) => {
    mockUseProjectsStore.mockReturnValue({
      projects: [],
      isLoading: false,
      isSubmitting: false,
      error: null,
      fetchProjects,
      createProject,
      updateProject,
      deleteProject,
      ...overrides,
    });
  };

  beforeEach(() => {
    jest.clearAllMocks();
    fetchProjects.mockResolvedValue(undefined);
    createProject.mockResolvedValue(activeProject);
    updateProject.mockResolvedValue(activeProject);
    deleteProject.mockResolvedValue(undefined);
    setStore();
  });

  it('should fetch projects on mount', () => {
    render(<ProjectList />);

    expect(fetchProjects).toHaveBeenCalledTimes(1);
  });

  it('should render the loading status', () => {
    setStore({ isLoading: true, projects: [activeProject] });
    render(<ProjectList />);

    expect(screen.getByRole('status')).toHaveTextContent('Loading projects…');
    expect(screen.queryByText('Deep Work')).not.toBeInTheDocument();
  });

  it('should render the error state with a working Retry', async () => {
    const user = userEvent.setup();
    setStore({ error: 'Server exploded' });
    render(<ProjectList />);

    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Server exploded');
    expect(fetchProjects).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: 'Retry' }));

    expect(fetchProjects).toHaveBeenCalledTimes(2);
  });

  it('should render the empty state with the New project action', async () => {
    const user = userEvent.setup();
    render(<ProjectList />);

    expect(screen.getByText('No projects yet')).toBeInTheDocument();
    expect(
      screen.getByText('Create your first project to organize your time blocks.')
    ).toBeInTheDocument();

    const [newProjectButton] = screen.getAllByRole('button', { name: 'New project' });
    await user.click(newProjectButton);

    expect(screen.getByRole('heading', { name: 'New project' })).toBeInTheDocument();
  });

  it('should render active and archived projects in server order', () => {
    setStore({ projects: [activeProject, archivedProject] });
    render(<ProjectList />);

    const headings = screen.getAllByRole('heading', { level: 2 });
    expect(headings.map((heading) => heading.textContent)).toEqual(['Deep Work', 'Reading']);
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Archived')).toBeInTheDocument();
  });

  it('should open the edit form for a project', async () => {
    const user = userEvent.setup();
    setStore({ projects: [activeProject] });
    render(<ProjectList />);

    await user.click(screen.getByRole('button', { name: 'Edit Deep Work' }));

    expect(screen.getByRole('heading', { name: 'Edit project' })).toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toHaveValue('Deep Work');
  });

  it('should archive a project through the store', async () => {
    const user = userEvent.setup();
    setStore({ projects: [activeProject] });
    render(<ProjectList />);

    await user.click(screen.getByRole('button', { name: 'Archive Deep Work' }));
    await user.click(screen.getByRole('button', { name: 'Archive' }));

    await waitFor(() => {
      expect(updateProject).toHaveBeenCalledWith(activeProject.id, { status: 'ARCHIVED' });
    });
  });

  it('should open the delete dialog from a card', async () => {
    const user = userEvent.setup();
    setStore({ projects: [activeProject] });
    render(<ProjectList />);

    await user.click(screen.getByRole('button', { name: 'Delete Deep Work' }));

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Delete project')).toBeInTheDocument();
  });

  it('should delete a project through the store when confirmed', async () => {
    const user = userEvent.setup();
    setStore({ projects: [activeProject] });
    render(<ProjectList />);

    await user.click(screen.getByRole('button', { name: 'Delete Deep Work' }));
    await user.click(screen.getByRole('button', { name: 'Delete' }));

    await waitFor(() => {
      expect(deleteProject).toHaveBeenCalledWith(activeProject.id);
    });
  });

  it('should close the create form and restore focus after a successful create', async () => {
    const user = userEvent.setup();
    render(<ProjectList />);

    await user.click(screen.getAllByRole('button', { name: 'New project' })[0]);
    await user.type(screen.getByLabelText('Name'), 'Deep Work');
    await user.click(screen.getByRole('button', { name: 'Use color #0F766E' }));
    await user.click(screen.getByRole('button', { name: 'Create project' }));

    await waitFor(() => {
      expect(createProject).toHaveBeenCalled();
    });
    expect(screen.queryByRole('heading', { name: 'New project' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'New project' })[0]).toHaveFocus();
  });

  it('should close the edit form and refocus the edited card action', async () => {
    const user = userEvent.setup();
    setStore({ projects: [activeProject] });
    render(<ProjectList />);

    await user.click(screen.getByRole('button', { name: 'Edit Deep Work' }));
    const nameInput = screen.getByLabelText('Name');
    await user.clear(nameInput);
    await user.type(nameInput, 'Deep Work 2');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => {
      expect(updateProject).toHaveBeenCalledWith(activeProject.id, { name: 'Deep Work 2' });
    });
    expect(screen.queryByRole('heading', { name: 'Edit project' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Edit Deep Work' })).toHaveFocus();
  });

  it('should reactivate an archived project through the store', async () => {
    const user = userEvent.setup();
    setStore({ projects: [archivedProject] });
    render(<ProjectList />);

    await user.click(screen.getByRole('button', { name: 'Reactivate Reading' }));

    await waitFor(() => {
      expect(updateProject).toHaveBeenCalledWith(archivedProject.id, { status: 'ACTIVE' });
    });
  });

  it('should cancel the delete dialog and refocus the trigger', async () => {
    const user = userEvent.setup();
    setStore({ projects: [activeProject] });
    render(<ProjectList />);

    await user.click(screen.getByRole('button', { name: 'Delete Deep Work' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(deleteProject).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Delete Deep Work' })).toHaveFocus();
  });
});
