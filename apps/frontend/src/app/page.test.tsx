import { render, screen } from '@testing-library/react';
import HomePage from './page';

describe('HomePage', () => {
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
    const main = screen.getByRole('main');
    expect(main).toBeInTheDocument();
  });

  it('renders the system description', () => {
    render(<HomePage />);
    expect(screen.getByText('Time-Centered Productivity System')).toBeInTheDocument();
  });

  it('renders with correct heading level', () => {
    render(<HomePage />);
    const heading = screen.getByRole('heading', { level: 1 });
    expect(heading).toBeInTheDocument();
    expect(heading).toHaveTextContent('Kairos');
  });
});