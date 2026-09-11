import { render, screen } from '@testing-library/react';
import { ProtectedRoute } from './ProtectedRoute';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

const mockUseAuthStore = useAuthStore as unknown as jest.Mock;

const mockPush = jest.fn();
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}));

describe('ProtectedRoute', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render children when authenticated', () => {
    mockUseAuthStore.mockReturnValue({ user: { id: '1', email: 'a@b.com' }, isLoading: false });

    render(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('should redirect to home when not authenticated', () => {
    mockUseAuthStore.mockReturnValue({ user: null, isLoading: false });

    render(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    expect(mockPush).toHaveBeenCalledWith('/');
  });

  it('should show loading state while checking auth', () => {
    mockUseAuthStore.mockReturnValue({ user: null, isLoading: true });

    render(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
    expect(mockPush).not.toHaveBeenCalled();
  });
});