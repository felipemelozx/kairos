import type { Metadata } from 'next';
import { Inter, IBM_Plex_Mono } from 'next/font/google';
import { AuthProvider } from '@/components/auth/AuthProvider';
import './globals.css';

const display = Inter({
  subsets: ['latin'],
  variable: '--font-display',
  fallback: ['Familjen Grotesk', 'Helvetica Neue', 'Arial', 'system-ui', 'sans-serif'],
});

const body = Inter({
  subsets: ['latin'],
  variable: '--font-body',
  fallback: ['Onest', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
});

const mono = IBM_Plex_Mono({
  subsets: ['latin'],
  weight: ['400', '500', '600'],
  variable: '--font-mono',
  fallback: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace'],
});

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
    <html lang="en" suppressHydrationWarning>
      <body
        suppressHydrationWarning
        className={`${display.variable} ${body.variable} ${mono.variable}`}
      >
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
