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
});