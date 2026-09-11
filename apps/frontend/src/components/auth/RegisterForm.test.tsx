import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RegisterForm } from './RegisterForm';
import { ApiError } from '@/lib/api';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

const mockUseAuthStore = useAuthStore as unknown as jest.Mock;

describe('RegisterForm', () => {
  const mockRegister = jest.fn();
  const mockOnSwitchToLogin = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAuthStore.mockReturnValue({
      register: mockRegister,
      isSubmitting: false,
    });
  });

  const renderForm = () =>
    render(<RegisterForm onSwitchToLogin={mockOnSwitchToLogin} />);

  it('should render name, email, and password fields', () => {
    renderForm();

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('should call register on submit with form data', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith({
        name: 'John Doe',
        email: 'john@example.com',
        password: 'password123',
      });
    });
  });

  it('should show validation error when password is too short', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.type(screen.getByLabelText(/name/i), 'John');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'short');
    await user.click(screen.getByRole('button', { name: /register/i }));

    expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('should show inline errors for empty fields and not call register', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole('button', { name: /register/i }));

    expect(screen.getByText('Enter your name.')).toBeInTheDocument();
    expect(screen.getByText('Enter your email address.')).toBeInTheDocument();
    expect(screen.getByText('Enter a password.')).toBeInTheDocument();
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('should focus the first invalid field on submit', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole('button', { name: /register/i }));

    expect(screen.getByLabelText(/name/i)).toHaveFocus();
  });

  it('should show an inline email error for a malformed email and not call register', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.type(screen.getByLabelText(/name/i), 'John');
    await user.type(screen.getByLabelText(/email/i), 'not-an-email');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /register/i }));

    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument();
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('should reject a name longer than 100 characters and not call register', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.type(screen.getByLabelText(/name/i), 'a'.repeat(101));
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /register/i }));

    expect(screen.getByText('Name must be 100 characters or fewer.')).toBeInTheDocument();
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('should associate inline errors with their inputs for assistive technology', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole('button', { name: /register/i }));

    const emailInput = screen.getByLabelText(/email/i);
    expect(emailInput).toHaveAttribute('aria-invalid', 'true');
    expect(emailInput).toHaveAttribute('aria-describedby', 'register-email-error');

    const errorNode = document.getElementById('register-email-error');
    expect(errorNode).toHaveTextContent('Enter your email address.');
    expect(errorNode).toHaveAttribute('role', 'alert');
  });

  it('should clear a field error when the user edits that field', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole('button', { name: /register/i }));
    expect(screen.getByText('Enter your name.')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/name/i), 'John');

    await waitFor(() => {
      expect(screen.queryByText('Enter your name.')).not.toBeInTheDocument();
    });
    expect(screen.getByLabelText(/name/i)).toHaveAttribute('aria-invalid', 'false');
  });

  it('should map API field details to inline errors', async () => {
    const user = userEvent.setup();
    mockRegister.mockRejectedValue(
      new ApiError(400, 'Request validation failed', 'VALIDATION_ERROR', {
        email: 'Email already registered',
      })
    );
    renderForm();

    await user.type(screen.getByLabelText(/name/i), 'John');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(screen.getByText('Email already registered')).toBeInTheDocument();
    });
    expect(screen.getByLabelText(/email/i)).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText(/email/i)).toHaveFocus();
    expect(screen.queryByText('Request validation failed')).not.toBeInTheDocument();
  });

  it('should show error message on register failure', async () => {
    const user = userEvent.setup();
    mockRegister.mockRejectedValue(new Error('Email already registered'));
    renderForm();

    await user.type(screen.getByLabelText(/name/i), 'John');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(screen.getByText(/email already registered/i)).toBeInTheDocument();
    });
  });

  it('should call onSwitchToLogin when clicking login link', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByText(/sign in/i));

    expect(mockOnSwitchToLogin).toHaveBeenCalled();
  });
});
