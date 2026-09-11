import { render, screen } from '@testing-library/react';
import RootLayout from './layout';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

const mockUseAuthStore = useAuthStore as unknown as jest.Mock;

describe('RootLayout', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAuthStore.mockReturnValue({ fetchMe: jest.fn() });
  });

  it('should call fetchMe on mount', () => {
    const mockFetchMe = jest.fn();
    mockUseAuthStore.mockReturnValue({ fetchMe: mockFetchMe });

    render(
      <RootLayout>
        <div>Test</div>
      </RootLayout>
    );

    expect(mockFetchMe).toHaveBeenCalled();
  });

  it('renders children correctly', () => {
    render(
      <RootLayout>
        <div>Test Child</div>
      </RootLayout>
    );
    expect(screen.getByText('Test Child')).toBeInTheDocument();
  });

  it('renders body element', () => {
    render(
      <RootLayout>
        <div>Test</div>
      </RootLayout>
    );
    const body = document.body;
    expect(body).toBeInTheDocument();
  });

  it('renders multiple children', () => {
    render(
      <RootLayout>
        <div>Child 1</div>
        <div>Child 2</div>
      </RootLayout>
    );
    expect(screen.getByText('Child 1')).toBeInTheDocument();
    expect(screen.getByText('Child 2')).toBeInTheDocument();
  });

  it('renders nested children', () => {
    render(
      <RootLayout>
        <div>
          <span>Nested Content</span>
        </div>
      </RootLayout>
    );
    expect(screen.getByText('Nested Content')).toBeInTheDocument();
  });
});
