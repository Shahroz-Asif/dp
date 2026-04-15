import { createContext, useCallback, useContext, useState } from 'react';
import type { ReactNode } from 'react';
import { credentialsSetter } from '../api/client';
import client from '../api/client';
import type { UserResponse, UserRole } from '../types/api';

interface Credentials {
  username: string;
  password: string;
}

interface AuthContextType {
  credentials: Credentials | null;
  isAuthenticated: boolean;
  role: UserRole | null;
  patientProfileId: number | null;
  /** Set credentials, call /api/auth/me to get role, throws on 401 */
  login: (username: string, password: string) => Promise<void>;
  /** Clear credentials and auth state */
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [role, setRole] = useState<UserRole | null>(null);
  const [patientProfileId, setPatientProfileId] = useState<number | null>(null);

  const login = useCallback(async (username: string, password: string) => {
    credentialsSetter.set(username, password);
    try {
      const me = await client.get<UserResponse>('/auth/me').then((r) => r.data);
      setCredentials({ username, password });
      setRole(me.role);
      setPatientProfileId(me.patientProfileId ?? null);
    } catch {
      credentialsSetter.clear();
      throw new Error('Invalid username or password.');
    }
  }, []);

  const logout = useCallback(() => {
    credentialsSetter.clear();
    setCredentials(null);
    setRole(null);
    setPatientProfileId(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{ credentials, isAuthenticated: credentials !== null, role, patientProfileId, login, logout }}
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
