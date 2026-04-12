import { createContext, useCallback, useContext, useState } from 'react';
import type { ReactNode } from 'react';
import { credentialsSetter } from '../api/client';

interface Credentials {
  username: string;
  password: string;
}

interface AuthContextType {
  credentials: Credentials | null;
  isAuthenticated: boolean;
  /** Set credentials and mark as authenticated */
  login: (username: string, password: string) => void;
  /** Clear credentials and mark as unauthenticated */
  logout: () => void;
}

/**
 * Observer Pattern — AuthContext acts as the observable store for
 * authentication state. Components subscribe via useAuth() and re-render
 * whenever credentials change. Side-effects (updating the Axios header store)
 * are pushed into the login/logout actions so subscribers stay decoupled from
 * the HTTP layer.
 */
const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [credentials, setCredentials] = useState<Credentials | null>(null);

  const login = useCallback((username: string, password: string) => {
    credentialsSetter.set(username, password);
    setCredentials({ username, password });
  }, []);

  const logout = useCallback(() => {
    credentialsSetter.clear();
    setCredentials(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{ credentials, isAuthenticated: credentials !== null, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

/** Hook for consuming auth state anywhere in the tree */
export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>');
  return ctx;
}
