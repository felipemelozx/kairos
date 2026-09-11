import { readFileSync, existsSync } from 'fs';
import { join } from 'path';

interface YamlLike {
  load: (input: string) => unknown;
}

const yaml = require('js-yaml') as YamlLike;

const DESIGN_PATH = join(__dirname, '../../DESIGN.md');
const TAILWIND_PATH = join(__dirname, '../../tailwind.config.ts');

interface DesignFrontmatter {
  colors: Record<string, string>;
  typography: Record<string, { fontFamily?: string; fontSize?: string; fontWeight?: number; lineHeight?: number | string; letterSpacing?: string }>;
  rounded: Record<string, string>;
  spacing: Record<string, string>;
}

function readDesignFrontmatter(): DesignFrontmatter {
  const raw = readFileSync(DESIGN_PATH, 'utf8');
  const match = raw.match(/^---\n([\s\S]*?)\n---/);
  if (!match) throw new Error('DESIGN.md is missing YAML frontmatter');
  return yaml.load(match[1]) as DesignFrontmatter;
}

function readTailwindSource(): string {
  return readFileSync(TAILWIND_PATH, 'utf8');
}

function requireDesign(): DesignFrontmatter | null {
  if (!existsSync(DESIGN_PATH)) return null;
  try {
    return readDesignFrontmatter();
  } catch {
    return null;
  }
}

describe('DS-001 design tokens', () => {
  const design = requireDesign();
  const tailwind = readTailwindSource();

  const source = design ?? {
    colors: {
      accent: '#0F766E',
      'accent-hover': '#115E59',
      'accent-active': '#0A4F49',
      'accent-subtle': '#E6F4F2',
      'accent-contrast': '#FFFFFF',
      canvas: '#FAFAF9',
      surface: '#FFFFFF',
      'surface-muted': '#F4F4F2',
      border: '#E7E5E4',
      'border-strong': '#D6D3D1',
      ink: '#0A0A0A',
      'ink-secondary': '#52525B',
      'ink-muted': '#A1A1AA',
      'dark-block': '#0B0B0C',
      success: '#15803D',
      'success-subtle': '#DCFCE7',
      warning: '#B45309',
      'warning-subtle': '#FEF3C7',
      danger: '#B91C1C',
      'danger-subtle': '#FEE2E2',
      info: '#1D4ED8',
      'info-subtle': '#DBEAFE',
    },
    typography: {
      display: { fontFamily: 'Familjen Grotesk', fontSize: 'clamp(2.75rem, 5vw, 4.5rem)', fontWeight: 700, lineHeight: 1.02, letterSpacing: '-0.02em' },
      headline: { fontFamily: 'Familjen Grotesk', fontSize: 'clamp(1.875rem, 3vw, 2.75rem)', fontWeight: 650, lineHeight: 1.1, letterSpacing: '-0.01em' },
      title: { fontFamily: 'Familjen Grotesk', fontSize: '1.25rem', fontWeight: 600, lineHeight: 1.3 },
      body: { fontFamily: 'Onest', fontSize: '1rem', fontWeight: 400, lineHeight: 1.6 },
      label: { fontFamily: 'JetBrains Mono', fontSize: '0.75rem', fontWeight: 600, lineHeight: 1.2, letterSpacing: '0.12em' },
      button: { fontFamily: 'Onest', fontSize: '0.9375rem', fontWeight: 600, lineHeight: 1, letterSpacing: '0.01em' },
      data: { fontFamily: 'JetBrains Mono', fontSize: '0.875rem', fontWeight: 500, lineHeight: 1.4 },
    },
    rounded: { sm: '6px', md: '10px', lg: '16px', xl: '24px', pill: '999px' },
    spacing: { xs: '4px', sm: '8px', md: '16px', lg: '24px', xl: '32px', '2xl': '48px', '3xl': '80px', '4xl': '120px' },
  };

  describe('colors', () => {
    for (const [slug, hex] of Object.entries(source.colors)) {
      it(`exposes ${slug} = ${hex}`, () => {
        expect(tailwind).toContain(hex);
      });
    }
  });

  describe('borderRadius', () => {
    for (const [slug, value] of Object.entries(source.rounded)) {
      it(`exposes rounded.${slug} = ${value}`, () => {
        expect(tailwind).toContain(value);
      });
    }
  });

  describe('spacing', () => {
    for (const [slug, value] of Object.entries(source.spacing)) {
      it(`exposes spacing.${slug} = ${value}`, () => {
        expect(tailwind).toContain(value);
      });
    }
  });

  describe('font families', () => {
    const families = new Set(
      Object.values(source.typography)
        .map((t) => t.fontFamily?.split(',')[0].trim())
        .filter((f): f is string => Boolean(f))
    );
    for (const family of families) {
      it(`references font family ${family}`, () => {
        expect(tailwind.toLowerCase()).toContain(family.toLowerCase());
      });
    }
  });

  describe('font sizes', () => {
    for (const [role, t] of Object.entries(source.typography)) {
      if (!t.fontSize) continue;
      it(`exposes font size ${role} = ${t.fontSize}`, () => {
        expect(tailwind).toContain(t.fontSize);
      });
    }
  });

  describe('typography font weights', () => {
    for (const [role, t] of Object.entries(source.typography)) {
      if (!t.fontWeight) continue;
      it(`exposes font weight ${role} = ${t.fontWeight}`, () => {
        expect(tailwind).toContain(String(t.fontWeight));
      });
    }
  });

  describe('shadows', () => {
    const shadows = [
      '0 1px 2px rgba(10,10,10,.04)',
      '0 1px 3px rgba(10,10,10,.06), 0 1px 2px rgba(10,10,10,.04)',
      '0 4px 12px rgba(10,10,10,.06), 0 2px 4px rgba(10,10,10,.04)',
      '0 12px 28px rgba(10,10,10,.10), 0 4px 8px rgba(10,10,10,.05)',
      '0 24px 48px -12px rgba(10,10,10,.18)',
      '0 0 0 3px rgba(15,118,110,.25)',
    ];
    for (const shadow of shadows) {
      it(`exposes shadow ${shadow.slice(0, 40)}...`, () => {
        expect(tailwind).toContain(shadow);
      });
    }
  });

  describe('CSS custom properties', () => {
    it('globals.css defines --color-accent', () => {
      const css = readFileSync(join(__dirname, '../app/globals.css'), 'utf8');
      expect(css).toContain('--color-accent');
    });
  });
});