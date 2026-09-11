import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import HomePage from './page';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

const mockUseAuthStore = useAuthStore as unknown as jest.Mock;

describe('HomePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAuthStore.mockReturnValue({ user: null, isLoading: false, logout: jest.fn() });
  });

  it('renders the Kairos heading', () => {
    render(<HomePage />);
    expect(screen.getByRole('heading', { name: 'Kairos' })).toBeInTheDocument();
  });

  it('renders the product tagline', () => {
    render(<HomePage />);
    expect(screen.getByText('Only executed work counts as progress')).toBeInTheDocument();
  });

  it('renders the main container', () => {
    render(<HomePage />);
    expect(screen.getByRole('main')).toBeInTheDocument();
  });

  it('renders Continue with Google button', () => {
    render(<HomePage />);
    const googleButton = screen.getByRole('link', { name: /continue with google/i });
    expect(googleButton).toBeInTheDocument();
    expect(googleButton).toHaveAttribute('href', '/api/oauth2/authorization/google');
  });

  it('shows the email login form by default', () => {
    render(<HomePage />);
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('switches between login and register forms', async () => {
    const user = userEvent.setup();
    render(<HomePage />);

    await user.click(screen.getByRole('button', { name: /create an account/i }));

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
  });

  it('shows logged in state with user name and logout', () => {
    mockUseAuthStore.mockReturnValue({
      user: { id: '1', email: 'test@example.com', name: 'Test User', provider: 'LOCAL', active: true, createdAt: '2026-01-01', avatarUrl: null },
      isLoading: false,
      logout: jest.fn(),
    });
    render(<HomePage />);

    expect(screen.getByText('Test User')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /logout/i })).toBeInTheDocument();
  });

  it('calls logout when clicking logout button', async () => {
    const mockLogout = jest.fn();
    mockUseAuthStore.mockReturnValue({
      user: { id: '1', email: 'test@example.com', name: 'Test User', provider: 'LOCAL', active: true, createdAt: '2026-01-01', avatarUrl: null },
      isLoading: false,
      logout: mockLogout,
    });
    const user = userEvent.setup();
    render(<HomePage />);

    await user.click(screen.getByRole('button', { name: /logout/i }));

    expect(mockLogout).toHaveBeenCalled();
  });
});