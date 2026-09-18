import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProjectForm } from './ProjectForm';
import { ApiError } from '@/lib/api';
import { Project } from '@/lib/projects-api';
import { useProjectsStore } from '@/stores/projects-store';

jest.mock('@/stores/projects-store');

const mockUseProjectsStore = useProjectsStore as unknown as jest.Mock;

const mockProject: Project = {
  id: '11111111-1111-1111-1111-111111111111',
  userId: '22222222-2222-2222-2222-222222222222',
  name: 'Deep Work',
  description: 'Focused work',
  color: '#0F766E',
  status: 'ACTIVE',
  createdAt: '2026-09-12T10:00:00Z',
};

describe('ProjectForm', () => {
  const mockCreate = jest.fn();
  const mockUpdate = jest.fn();
  const onCancel = jest.fn();
  const onSuccess = jest.fn();

  const renderCreate = () =>
    render(<ProjectForm mode="create" onCancel={onCancel} onSuccess={onSuccess} />);

  const renderEdit = (project: Project = mockProject) =>
    render(
      <ProjectForm
        mode="edit"
        project={project}
        onCancel={onCancel}
        onSuccess={onSuccess}
      />
    );

  beforeEach(() => {
    jest.clearAllMocks();
    mockCreate.mockResolvedValue(mockProject);
    mockUpdate.mockResolvedValue(mockProject);
    mockUseProjectsStore.mockReturnValue({
      createProject: mockCreate,
      updateProject: mockUpdate,
      isSubmitting: false,
    });
  });

  it('should show inline errors on empty submit and not call the store', async () => {
    const user = userEvent.setup();
    renderCreate();

    await user.click(screen.getByRole('button', { name: 'Create project' }));

    expect(screen.getByText('Enter a project name.')).toBeInTheDocument();
    expect(screen.getByText('Choose a color.')).toBeInTheDocument();
    expect(mockCreate).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Name')).toHaveFocus();
  });

  it('should reject a malformed hex color and not call the store', async () => {
    const user = userEvent.setup();
    renderCreate();

    await user.type(screen.getByLabelText('Name'), 'Deep Work');
    await user.type(screen.getByLabelText('Hex color'), 'zzz');
    await user.click(screen.getByRole('button', { name: 'Create project' }));

    expect(
      screen.getByText('Enter a valid hex color (for example #0F766E).')
    ).toBeInTheDocument();
    expect(mockCreate).not.toHaveBeenCalled();
  });

  it('should reject a description longer than 500 characters', () => {
    renderCreate();

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Deep Work' } });
    fireEvent.change(screen.getByLabelText('Description (optional)'), {
      target: { value: 'a'.repeat(501) },
    });
    fireEvent.change(screen.getByLabelText('Hex color'), { target: { value: '#0F766E' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create project' }));

    expect(
      screen.getByText('Description must be 500 characters or fewer.')
    ).toBeInTheDocument();
    expect(mockCreate).not.toHaveBeenCalled();
  });

  it('should wire aria-invalid and aria-describedby on invalid fields', async () => {
    const user = userEvent.setup();
    renderCreate();

    await user.click(screen.getByRole('button', { name: 'Create project' }));

    const nameInput = screen.getByLabelText('Name');
    expect(nameInput).toHaveAttribute('aria-invalid', 'true');
    expect(nameInput).toHaveAttribute('aria-describedby', 'project-name-error');

    const errorNode = document.getElementById('project-name-error');
    expect(errorNode).toHaveTextContent('Enter a project name.');
    expect(errorNode).toHaveAttribute('role', 'alert');
  });

  it('should clear a field error when the user edits that field', async () => {
    const user = userEvent.setup();
    renderCreate();

    await user.click(screen.getByRole('button', { name: 'Create project' }));
    expect(screen.getByText('Enter a project name.')).toBeInTheDocument();

    await user.type(screen.getByLabelText('Name'), 'Deep Work');

    await waitFor(() => {
      expect(screen.queryByText('Enter a project name.')).not.toBeInTheDocument();
    });
    expect(screen.getByLabelText('Name')).toHaveAttribute('aria-invalid', 'false');
  });

  it('should call createProject with the payload on a valid submit', async () => {
    const user = userEvent.setup();
    renderCreate();

    await user.type(screen.getByLabelText('Name'), 'Deep Work');
    await user.type(screen.getByLabelText('Description (optional)'), 'Focused work');
    await user.click(screen.getByRole('button', { name: 'Use color #0F766E' }));
    await user.click(screen.getByRole('button', { name: 'Create project' }));

    await waitFor(() => {
      expect(mockCreate).toHaveBeenCalledWith({
        name: 'Deep Work',
        description: 'Focused work',
        color: '#0F766E',
      });
    });
    expect(onSuccess).toHaveBeenCalledTimes(1);
  });

  it('should call updateProject with only the changed fields in edit mode', async () => {
    const user = userEvent.setup();
    renderEdit();

    const nameInput = screen.getByLabelText('Name');
    await user.clear(nameInput);
    await user.type(nameInput, 'Deep Work 2');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledWith(mockProject.id, { name: 'Deep Work 2' });
    });
    expect(onSuccess).toHaveBeenCalledTimes(1);
  });

  it('should not call updateProject when nothing changed', async () => {
    const user = userEvent.setup();
    renderEdit();

    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(mockUpdate).not.toHaveBeenCalled();
    expect(onSuccess).toHaveBeenCalledTimes(1);
  });

  it('should map API field details to inline errors', async () => {
    const user = userEvent.setup();
    mockCreate.mockRejectedValue(
      new ApiError(400, 'Request validation failed', 'VALIDATION_ERROR', {
        name: 'Name already used',
      })
    );
    renderCreate();

    await user.type(screen.getByLabelText('Name'), 'Deep Work');
    await user.click(screen.getByRole('button', { name: 'Use color #0F766E' }));
    await user.click(screen.getByRole('button', { name: 'Create project' }));

    await waitFor(() => {
      expect(screen.getByText('Name already used')).toBeInTheDocument();
    });
    expect(screen.getByLabelText('Name')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('Name')).toHaveFocus();
    expect(screen.queryByText('Request validation failed')).not.toBeInTheDocument();
  });

  it('should render the fallback form-level alert for errors without details', async () => {
    const user = userEvent.setup();
    mockCreate.mockRejectedValue(new Error('Server exploded'));
    renderCreate();

    await user.type(screen.getByLabelText('Name'), 'Deep Work');
    await user.click(screen.getByRole('button', { name: 'Use color #0F766E' }));
    await user.click(screen.getByRole('button', { name: 'Create project' }));

    await waitFor(() => {
      expect(document.getElementById('project-form-error')).toHaveTextContent(
        'Server exploded'
      );
    });
  });

  it('should mark the submit button busy while submitting', () => {
    mockUseProjectsStore.mockReturnValue({
      createProject: mockCreate,
      updateProject: mockUpdate,
      isSubmitting: true,
    });
    renderCreate();

    expect(screen.getByRole('button', { name: 'Saving...' })).toBeDisabled();
  });

  it('should call onCancel when clicking Cancel', async () => {
    const user = userEvent.setup();
    renderCreate();

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('should reflect the selected preset with aria-pressed', async () => {
    const user = userEvent.setup();
    renderCreate();

    const preset = screen.getByRole('button', { name: 'Use color #0F766E' });
    expect(preset).toHaveAttribute('aria-pressed', 'false');

    await user.click(preset);

    expect(preset).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByLabelText('Hex color')).toHaveValue('#0F766E');
  });
});
