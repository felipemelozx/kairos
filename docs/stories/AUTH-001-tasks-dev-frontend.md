# Tasks: @dev frontend — AUTH-001

## Context

- Story: `docs/stories/AUTH-001-login.md`
- OpenAPI Spec: `docs/openapi/auth.yaml`
- Approach: **TDD (Red-Green-Refactor)**

---

## Phase 1: Setup

### Task 1.1: Dependencies

Verify `apps/frontend/package.json` has:

```json
{
  "dependencies": {
    "zustand": "^4.5.0",
    "axios": "^1.7.0"
  },
  "devDependencies": {
    "@testing-library/react": "^14.0.0",
    "@testing-library/jest-dom": "^6.0.0",
    "@testing-library/user-event": "^14.0.0"
  }
}
```

### Task 1.2: Environment Variables

Create `apps/frontend/.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## Phase 2: TDD Tasks

### Task 2.1: API Client

**🔴 RED — Write failing test first:**

Create `apps/frontend/src/lib/api.test.ts`:

```typescript
import { apiClient } from './api';

describe('apiClient', () => {
  it('should include credentials in requests', () => {
    expect(apiClient.defaults.withCredentials).toBe(true);
  });

  it('should have correct base URL', () => {
    expect(apiClient.defaults.baseURL).toBe(process.env.NEXT_PUBLIC_API_URL);
  });
});
```

**Run test → FAILS** (no api client)

**🟢 GREEN — Implement minimum to pass:**

Create `apps/frontend/src/lib/api.ts`:

```typescript
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});
```

**Run test → PASSES**

**🔵 REFACTOR:**
- Add error interceptor for 401 responses

---

### Task 2.2: Auth API Functions

**🔴 RED:**

Create `apps/frontend/src/lib/auth-api.test.ts`:

```typescript
import { authApi } from './auth-api';
import { apiClient } from './api';

jest.mock('./api');

describe('authApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('register', () => {
    it('should call register endpoint with correct data', async () => {
      const mockResponse = { data: { success: true, data: { id: '1', email: 'test@example.com', name: 'Test' } } };
      (apiClient.post as jest.Mock).mockResolvedValue(mockResponse);

      const result = await authApi.register({ email: 'test@example.com', password: 'password123', name: 'Test' });

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/register', {
        email: 'test@example.com',
        password: 'password123',
        name: 'Test',
      });
      expect(result).toEqual(mockResponse.data.data);
    });
  });

  describe('login', () => {
    it('should call login endpoint with correct data', async () => {
      const mockResponse = { data: { success: true, data: { id: '1', email: 'test@example.com', name: 'Test' } } };
      (apiClient.post as jest.Mock).mockResolvedValue(mockResponse);

      const result = await authApi.login({ email: 'test@example.com', password: 'password123' });

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/login', {
        email: 'test@example.com',
        password: 'password123',
      });
      expect(result).toEqual(mockResponse.data.data);
    });
  });

  describe('logout', () => {
    it('should call logout endpoint', async () => {
      (apiClient.post as jest.Mock).mockResolvedValue({ data: { success: true } });

      await authApi.logout();

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/logout');
    });
  });

  describe('me', () => {
    it('should call me endpoint', async () => {
      const mockResponse = { data: { success: true, data: { id: '1', email: 'test@example.com', name: 'Test' } } };
      (apiClient.get as jest.Mock).mockResolvedValue(mockResponse);

      const result = await authApi.me();

      expect(apiClient.get).toHaveBeenCalledWith('/api/auth/me');
      expect(result).toEqual(mockResponse.data.data);
    });
  });
});
```

**Run test → FAILS**

**🟢 GREEN:**

Create `apps/frontend/src/lib/auth-api.ts`:

```typescript
import { apiClient } from './api';

export interface RegisterData {
  email: string;
  password: string;
  name: string;
}

export interface LoginData {
  email: string;
  password: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  avatarUrl?: string;
  provider: 'LOCAL' | 'GOOGLE';
  active: boolean;
  createdAt: string;
}

export const authApi = {
  register: async (data: RegisterData): Promise<User> => {
    const response = await apiClient.post('/api/auth/register', data);
    return response.data.data;
  },

  login: async (data: LoginData): Promise<User> => {
    const response = await apiClient.post('/api/auth/login', data);
    return response.data.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/api/auth/logout');
  },

  me: async (): Promise<User> => {
    const response = await apiClient.get('/api/auth/me');
    return response.data.data;
  },
};
```

**Run test → PASSES**

**🔵 REFACTOR:**
- Add error handling
- Add types for API responses

---

### Task 2.3: Auth Store (Zustand)

**🔴 RED:**

Create `apps/frontend/src/stores/auth-store.test.ts`:

```typescript
import { useAuthStore } from './auth-store';
import { authApi } from '@/lib/auth-api';

jest.mock('@/lib/auth-api');

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, isLoading: false });
    jest.clearAllMocks();
  });

  describe('login', () => {
    it('should set user after successful login', async () => {
      const mockUser = { id: '1', email: 'test@example.com', name: 'Test', provider: 'LOCAL', active: true, createdAt: '2026-01-01' };
      (authApi.login as jest.Mock).mockResolvedValue(mockUser);

      await useAuthStore.getState().login({ email: 'test@example.com', password: 'password123' });

      expect(useAuthStore.getState().user).toEqual(mockUser);
    });

    it('should throw error on failed login', async () => {
      (authApi.login as jest.Mock).mockRejectedValue(new Error('Invalid credentials'));

      await expect(
        useAuthStore.getState().login({ email: 'test@example.com', password: 'wrong' })
      ).rejects.toThrow('Invalid credentials');

      expect(useAuthStore.getState().user).toBeNull();
    });
  });

  describe('register', () => {
    it('should set user after successful register', async () => {
      const mockUser = { id: '1', email: 'test@example.com', name: 'Test', provider: 'LOCAL', active: true, createdAt: '2026-01-01' };
      (authApi.register as jest.Mock).mockResolvedValue(mockUser);

      await useAuthStore.getState().register({ email: 'test@example.com', password: 'password123', name: 'Test' });

      expect(useAuthStore.getState().user).toEqual(mockUser);
    });
  });

  describe('logout', () => {
    it('should clear user after logout', async () => {
      useAuthStore.setState({ user: { id: '1', email: 'test@example.com', name: 'Test', provider: 'LOCAL', active: true, createdAt: '2026-01-01' } });
      (authApi.logout as jest.Mock).mockResolvedValue(undefined);

      await useAuthStore.getState().logout();

      expect(useAuthStore.getState().user).toBeNull();
    });
  });

  describe('fetchMe', () => {
    it('should set user when authenticated', async () => {
      const mockUser = { id: '1', email: 'test@example.com', name: 'Test', provider: 'LOCAL', active: true, createdAt: '2026-01-01' };
      (authApi.me as jest.Mock).mockResolvedValue(mockUser);

      await useAuthStore.getState().fetchMe();

      expect(useAuthStore.getState().user).toEqual(mockUser);
    });

    it('should not set user when not authenticated', async () => {
      (authApi.me as jest.Mock).mockRejectedValue(new Error('Not authenticated'));

      await useAuthStore.getState().fetchMe();

      expect(useAuthStore.getState().user).toBeNull();
    });
  });
});
```

**Run test → FAILS**

**🟢 GREEN:**

Create `apps/frontend/src/stores/auth-store.ts`:

```typescript
import { create } from 'zustand';
import { authApi, User, LoginData, RegisterData } from '@/lib/auth-api';

interface AuthState {
  user: User | null;
  isLoading: boolean;
  login: (data: LoginData) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  fetchMe: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isLoading: false,

  login: async (data: LoginData) => {
    set({ isLoading: true });
    try {
      const user = await authApi.login(data);
      set({ user, isLoading: false });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  register: async (data: RegisterData) => {
    set({ isLoading: true });
    try {
      const user = await authApi.register(data);
      set({ user, isLoading: false });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  logout: async () => {
    await authApi.logout();
    set({ user: null });
  },

  fetchMe: async () => {
    set({ isLoading: true });
    try {
      const user = await authApi.me();
      set({ user, isLoading: false });
    } catch (error) {
      set({ user: null, isLoading: false });
    }
  },
}));
```

**Run test → PASSES**

**🔵 REFACTOR:**
- Add error state to store

---

### Task 2.4: Login Form Component

**🔴 RED:**

Create `apps/frontend/src/components/auth/LoginForm.test.tsx`:

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoginForm } from './LoginForm';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

describe('LoginForm', () => {
  const mockLogin = jest.fn();
  const mockOnSwitchToRegister = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useAuthStore as unknown as jest.Mock).mockReturnValue({
      login: mockLogin,
      isLoading: false,
    });
  });

  it('should render email and password fields', () => {
    render(<LoginForm onSwitchToRegister={mockOnSwitchToRegister} />);

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('should call login on submit with form data', async () => {
    const user = userEvent.setup();
    render(<LoginForm onSwitchToRegister={mockOnSwitchToRegister} />);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123',
      });
    });
  });

  it('should show error message on login failure', async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValue(new Error('Invalid credentials'));
    render(<LoginForm onSwitchToRegister={mockOnSwitchToRegister} />);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /login/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
    });
  });

  it('should call onSwitchToRegister when clicking register link', async () => {
    const user = userEvent.setup();
    render(<LoginForm onSwitchToRegister={mockOnSwitchToRegister} />);

    await user.click(screen.getByText(/register/i));

    expect(mockOnSwitchToRegister).toHaveBeenCalled();
  });

  it('should disable submit button while loading', () => {
    (useAuthStore as unknown as jest.Mock).mockReturnValue({
      login: mockLogin,
      isLoading: true,
    });
    render(<LoginForm onSwitchToRegister={mockOnSwitchToRegister} />);

    expect(screen.getByRole('button', { name: /login/i })).toBeDisabled();
  });
});
```

**Run test → FAILS**

**🟢 GREEN:**

Create `apps/frontend/src/components/auth/LoginForm.tsx`:

```typescript
'use client';

import { useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';

interface LoginFormProps {
  onSwitchToRegister: () => void;
}

export function LoginForm({ onSwitchToRegister }: LoginFormProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login, isLoading } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await login({ email, password });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>
      <div>
        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          minLength={8}
        />
      </div>
      {error && <p className="text-red-500">{error}</p>}
      <button type="submit" disabled={isLoading}>
        {isLoading ? 'Logging in...' : 'Login'}
      </button>
      <p>
        Don't have an account?{' '}
        <button type="button" onClick={onSwitchToRegister}>
          Register
        </button>
      </p>
    </form>
  );
}
```

**Run test → PASSES**

**🔵 REFACTOR:**
- Add Tailwind styles
- Add form validation

---

### Task 2.5: Register Form Component

**🔴 RED:**

Create `apps/frontend/src/components/auth/RegisterForm.test.tsx`:

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RegisterForm } from './RegisterForm';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

describe('RegisterForm', () => {
  const mockRegister = jest.fn();
  const mockOnSwitchToLogin = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useAuthStore as unknown as jest.Mock).mockReturnValue({
      register: mockRegister,
      isLoading: false,
    });
  });

  it('should render name, email, and password fields', () => {
    render(<RegisterForm onSwitchToLogin={mockOnSwitchToLogin} />);

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('should call register on submit with form data', async () => {
    const user = userEvent.setup();
    render(<RegisterForm onSwitchToLogin={mockOnSwitchToLogin} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith({
        name: 'John Doe',
        email: 'john@example.com',
        password: 'password123',
      });
    });
  });

  it('should show error when password is too short', async () => {
    const user = userEvent.setup();
    render(<RegisterForm onSwitchToLogin={mockOnSwitchToLogin} />);

    await user.type(screen.getByLabelText(/name/i), 'John');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'short');
    await user.click(screen.getByRole('button', { name: /register/i }));

    expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
  });

  it('should show error message on register failure', async () => {
    const user = userEvent.setup();
    mockRegister.mockRejectedValue(new Error('Email already registered'));
    render(<RegisterForm onSwitchToLogin={mockOnSwitchToLogin} />);

    await user.type(screen.getByLabelText(/name/i), 'John');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(screen.getByText(/email already registered/i)).toBeInTheDocument();
    });
  });

  it('should call onSwitchToLogin when clicking login link', async () => {
    const user = userEvent.setup();
    render(<RegisterForm onSwitchToLogin={mockOnSwitchToLogin} />);

    await user.click(screen.getByText(/login/i));

    expect(mockOnSwitchToLogin).toHaveBeenCalled();
  });
});
```

**Run test → FAILS → GREEN → REFACTOR**

---

### Task 2.6: Landing Page

**🔴 RED:**

Update `apps/frontend/src/app/page.test.tsx`:

```typescript
import { render, screen } from '@testing-library/react';
import Page from './page';

describe('Landing Page', () => {
  it('should render Login with Google button', () => {
    render(<Page />);
    expect(screen.getByRole('button', { name: /login with google/i })).toBeInTheDocument();
  });

  it('should render Login with Email button', () => {
    render(<Page />);
    expect(screen.getByRole('button', { name: /login with email/i })).toBeInTheDocument();
  });

  it('should show login form when clicking Login with Email', async () => {
    const user = userEvent.setup();
    render(<Page />);

    await user.click(screen.getByRole('button', { name: /login with email/i }));

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
  });

  it('should redirect to Google OAuth when clicking Login with Google', () => {
    render(<Page />);
    const googleButton = screen.getByRole('button', { name: /login with google/i });
    expect(googleButton.closest('a')).toHaveAttribute('href', '/oauth2/authorization/google');
  });
});
```

**Run test → FAILS → GREEN → REFACTOR**

---

### Task 2.7: Protected Route Component

**🔴 RED:**

Create `apps/frontend/src/components/ProtectedRoute.test.tsx`:

```typescript
import { render, screen } from '@testing-library/react';
import { ProtectedRoute } from './ProtectedRoute';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

describe('ProtectedRoute', () => {
  it('should render children when authenticated', () => {
    (useAuthStore as unknown as jest.Mock).mockReturnValue({ user: { id: '1' }, isLoading: false });

    render(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('should redirect to home when not authenticated', () => {
    (useAuthStore as unknown as jest.Mock).mockReturnValue({ user: null, isLoading: false });

    render(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('should show loading state while checking auth', () => {
    (useAuthStore as unknown as jest.Mock).mockReturnValue({ user: null, isLoading: true });

    render(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });
});
```

**Run test → FAILS → GREEN → REFACTOR**

---

### Task 2.8: Auth on App Load

**🔴 RED:**

Update `apps/frontend/src/app/layout.test.tsx`:

```typescript
import { render, waitFor } from '@testing-library/react';
import RootLayout from './layout';
import { useAuthStore } from '@/stores/auth-store';

jest.mock('@/stores/auth-store');

describe('RootLayout', () => {
  it('should call fetchMe on mount', () => {
    const mockFetchMe = jest.fn();
    (useAuthStore as unknown as jest.Mock).mockReturnValue({ fetchMe: mockFetchMe });

    render(<RootLayout><div>Test</div></RootLayout>);

    expect(mockFetchMe).toHaveBeenCalled();
  });
});
```

**Run test → FAILS → GREEN → REFACTOR**

---

## Quality Gates

- [x] All unit tests passing
- [ ] 80%+ coverage
- [x] Zero lint errors (`npm run lint`)
- [x] Zero typecheck errors (`npm run typecheck`)
- [x] All components accessible (ARIA labels)

---

## File List

| File | Purpose |
|------|---------|
| `src/lib/api.ts` | API client (fetch, CSRF-aware, `ApiError`) |
| `src/lib/csrf.ts` | CSRF token reader from cookie |
| `src/lib/auth-api.ts` | Auth API functions (register/login/logout/refresh/me) |
| `src/stores/auth-store.ts` | Zustand store (`user`, `login`, `register`, `logout`, `fetchMe`) |
| `src/components/auth/LoginForm.tsx` | Email/password login form |
| `src/components/auth/RegisterForm.tsx` | Register form with validation |
| `src/components/auth/AuthProvider.tsx` | Calls `fetchMe` on app load |
| `src/components/ProtectedRoute.tsx` | Redirects unauthenticated users to `/` |
| `src/app/page.tsx` | Landing page with Google + Email login |
| `src/app/layout.tsx` | Root layout wrapping children in `AuthProvider` |

**Tests:** `src/lib/api.test.ts`, `src/lib/auth-api.test.ts`, `src/stores/auth-store.test.ts`, `src/components/auth/LoginForm.test.tsx`, `src/components/auth/RegisterForm.test.tsx`, `src/components/ProtectedRoute.test.tsx`, `src/app/page.test.tsx`, `src/app/layout.test.tsx`
