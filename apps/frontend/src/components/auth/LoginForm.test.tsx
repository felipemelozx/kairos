import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoginForm } from './LoginForm';
import { ApiError } from '@/lib/api';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

const mockUseAuthStore = useAuthStore as unknown as jest.Mock;

describe('LoginForm', () => {
  const mockLogin = jest.fn();
  const mockOnSwitchToRegister = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAuthStore.mockReturnValue({
      login: mockLogin,
      isSubmitting: false,
    });
  });

  const renderForm = () =>
    render(<LoginForm onSwitchToRegister={mockOnSwitchToRegister} />);

  it('should render email and password fields', () => {
    renderForm();

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('should call login on submit with form data', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123',
      });
    });
  });

  it('should show error message on login failure', async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValue(new Error('Invalid email or password'));
    renderForm();

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
    });
  });

  it('should show inline errors for empty fields and not call login', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole('button', { name: /login/i }));

    expect(screen.getByText('Enter your email address.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('should show an inline email error for a malformed email and not call login', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.type(screen.getByLabelText(/email/i), 'not-an-email');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /login/i }));

    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('should focus the first invalid field on submit', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole('button', { name: /login/i }));

    expect(screen.getByLabelText(/email/i)).toHaveFocus();
  });

  it('should associate inline errors with their inputs for assistive technology', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole('button', { name: /login/i }));

    const emailInput = screen.getByLabelText(/email/i);
    expect(emailInput).toHaveAttribute('aria-invalid', 'true');
    expect(emailInput).toHaveAttribute('aria-describedby', 'login-email-error');

    const errorNode = document.getElementById('login-email-error');
    expect(errorNode).toHaveTextContent('Enter your email address.');
    expect(errorNode).toHaveAttribute('role', 'alert');
  });

  it('should clear a field error when the user edits that field', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole('button', { name: /login/i }));
    expect(screen.getByText('Enter your email address.')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');

    await waitFor(() => {
      expect(screen.queryByText('Enter your email address.')).not.toBeInTheDocument();
    });
    expect(screen.getByLabelText(/email/i)).toHaveAttribute('aria-invalid', 'false');
  });

  it('should map API field details to inline errors', async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValue(
      new ApiError(400, 'Request validation failed', 'VALIDATION_ERROR', {
        email: 'Email already registered',
      })
    );
    renderForm();

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(screen.getByText('Email already registered')).toBeInTheDocument();
    });
    expect(screen.getByLabelText(/email/i)).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText(/email/i)).toHaveFocus();
    expect(screen.queryByText('Request validation failed')).not.toBeInTheDocument();
  });

  it('should call onSwitchToRegister when clicking register link', async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByText(/create an account/i));

    expect(mockOnSwitchToRegister).toHaveBeenCalled();
  });

  it('should disable submit button while loading', () => {
    mockUseAuthStore.mockReturnValue({
      login: mockLogin,
      isSubmitting: true,
    });
    renderForm();

    expect(screen.getByRole('button', { name: /logging in/i })).toBeDisabled();
  });
});
