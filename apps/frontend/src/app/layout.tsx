import type { Metadata } from 'next';
import { Familjen_Grotesk, Onest, JetBrains_Mono } from 'next/font/google';
import { AuthProvider } from '@/components/auth/AuthProvider';
import './globals.css';

const display = Familjen_Grotesk({
  subsets: ['latin'],
  variable: '--font-display',
  fallback: ['Helvetica Neue', 'Arial', 'system-ui', 'sans-serif'],
});

const body = Onest({
  subsets: ['latin'],
  variable: '--font-body',
  fallback: ['system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
});

const mono = JetBrains_Mono({
  subsets: ['latin'],
  variable: '--font-mono',
  fallback: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace'],
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
