import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Kairos - Time-Centered Productivity',
  description: 'Only executed work counts as progress',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
