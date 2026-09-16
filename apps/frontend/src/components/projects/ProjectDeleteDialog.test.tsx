import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProjectDeleteDialog } from './ProjectDeleteDialog';
import { Project } from '@/lib/projects-api';

const mockProject: Project = {
  id: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  name: 'Deep Work',
  description: 'Focused work',
  color: '#0F766E',
  status: 'ACTIVE',
  createdAt: '2026-09-12T10:00:00Z',
};

describe('ProjectDeleteDialog', () => {
  const onCancel = jest.fn();
  const onConfirm = jest.fn();

  const renderDialog = () =>
    render(
      <ProjectDeleteDialog project={mockProject} onCancel={onCancel} onConfirm={onConfirm} />
    );

  beforeEach(() => {
    jest.clearAllMocks();
    onConfirm.mockResolvedValue(undefined);
  });

  it('should expose an accessible modal dialog with the honest copy', () => {
    renderDialog();

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAttribute('aria-labelledby', 'project-delete-title');
    expect(dialog).toHaveAttribute('aria-describedby', 'project-delete-description');
    expect(screen.getByText('Delete project')).toBeInTheDocument();
    expect(
      screen.getByText(
        '“Deep Work” will be removed from your projects. This can’t be undone in the app.'
      )
    ).toBeInTheDocument();
    expect(screen.queryByText(/permanently removes/i)).not.toBeInTheDocument();
  });

  it('should move initial focus to Cancel', () => {
    renderDialog();

    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
  });

  it('should call onConfirm when clicking Delete', async () => {
    const user = userEvent.setup();
    renderDialog();

    await user.click(screen.getByRole('button', { name: 'Delete' }));

    await waitFor(() => {
      expect(onConfirm).toHaveBeenCalledTimes(1);
    });
  });

  it('should call onCancel when clicking Cancel without calling onConfirm', async () => {
    const user = userEvent.setup();
    renderDialog();

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('should cancel on Escape without calling onConfirm', async () => {
    const user = userEvent.setup();
    renderDialog();

    await user.keyboard('{Escape}');

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('should keep focus trapped between the dialog buttons', async () => {
    const user = userEvent.setup();
    renderDialog();

    await user.tab();
    expect(screen.getByRole('button', { name: 'Delete' })).toHaveFocus();

    await user.tab();
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
  });

  it('should show the fallback alert and stay open when confirm fails', async () => {
    const user = userEvent.setup();
    onConfirm.mockRejectedValue(new Error('failed'));
    renderDialog();

    await user.click(screen.getByRole('button', { name: 'Delete' }));

    await waitFor(() => {
      expect(
        screen.getByText('Could not delete the project. Please try again.')
      ).toBeInTheDocument();
    });
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });
});
