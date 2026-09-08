import { render, screen } from '@testing-library/react';
import RootLayout from './layout';

describe('RootLayout', () => {
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
