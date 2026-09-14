import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProjectCard } from './ProjectCard';
import { Project } from '@/lib/projects-api';

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

describe('ProjectCard', () => {
  const onEdit = jest.fn();
  const onArchive = jest.fn();
  const onReactivate = jest.fn();
  const onDelete = jest.fn();

  const renderCard = (project: Project) =>
    render(
      <ProjectCard
        project={project}
        onEdit={onEdit}
        onArchive={onArchive}
        onReactivate={onReactivate}
        onDelete={onDelete}
      />
    );

  beforeEach(() => {
    jest.clearAllMocks();
    onArchive.mockResolvedValue(undefined);
    onReactivate.mockResolvedValue(undefined);
  });

  it('should render the name, description, color marker and status', () => {
    renderCard(activeProject);

    expect(screen.getByRole('heading', { name: 'Deep Work' })).toBeInTheDocument();
    expect(screen.getByText('Focused work')).toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();

    const dot = screen.getByTestId('project-color');
    expect(dot).toHaveAttribute('aria-hidden', 'true');
    expect(dot.style.backgroundColor).toBe('rgb(15, 118, 110)');
  });

  it('should render the createdAt metadata with ink-secondary', () => {
    renderCard(activeProject);

    const metadata = screen.getByText('Created Sep 12, 2026');
    expect(metadata).toBeInTheDocument();
    expect(metadata.className).toContain('text-ink-secondary');
    expect(metadata.className).not.toContain('text-ink-muted');
  });

  it('should render an archived project with the neutral badge and no description', () => {
    renderCard(archivedProject);

    expect(screen.getByText('Archived')).toBeInTheDocument();
    expect(screen.queryByText('Focused work')).not.toBeInTheDocument();
  });

  it('should trigger onEdit when clicking Edit', async () => {
    const user = userEvent.setup();
    renderCard(activeProject);

    await user.click(screen.getByRole('button', { name: 'Edit Deep Work' }));

    expect(onEdit).toHaveBeenCalledWith(activeProject);
  });

  it('should confirm archive and call onArchive', async () => {
    const user = userEvent.setup();
    renderCard(activeProject);

    await user.click(screen.getByRole('button', { name: 'Archive Deep Work' }));
    expect(screen.getByText('Archive “Deep Work”?')).toBeInTheDocument();
    expect(screen.getByText('You can reactivate it later.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Archive' }));

    await waitFor(() => {
      expect(onArchive).toHaveBeenCalledWith(activeProject);
    });
  });

  it('should cancel archive without calling the handler', async () => {
    const user = userEvent.setup();
    renderCard(activeProject);

    await user.click(screen.getByRole('button', { name: 'Archive Deep Work' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onArchive).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Archive Deep Work' })).toBeInTheDocument();
  });

  it('should call onReactivate immediately for an archived project', async () => {
    const user = userEvent.setup();
    renderCard(archivedProject);

    await user.click(screen.getByRole('button', { name: 'Reactivate Reading' }));

    await waitFor(() => {
      expect(onReactivate).toHaveBeenCalledWith(archivedProject);
    });
  });

  it('should trigger onDelete when clicking Delete', async () => {
    const user = userEvent.setup();
    renderCard(activeProject);

    await user.click(screen.getByRole('button', { name: 'Delete Deep Work' }));

    expect(onDelete).toHaveBeenCalledWith(activeProject);
  });

  it('should show an alert when archive fails', async () => {
    const user = userEvent.setup();
    onArchive.mockRejectedValue(new Error('failed'));
    renderCard(activeProject);

    await user.click(screen.getByRole('button', { name: 'Archive Deep Work' }));
    await user.click(screen.getByRole('button', { name: 'Archive' }));

    await waitFor(() => {
      expect(
        screen.getByText('Could not update the project. Please try again.')
      ).toBeInTheDocument();
    });
  });
});
