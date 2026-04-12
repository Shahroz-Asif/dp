import { createContext, useCallback, useContext, useState } from 'react';
import type { ReactNode } from 'react';

export interface ProfileCondition {
  id: number;
  name: string;
}

interface PatientProfileContextType {
  /** Conditions the current patient has selected as their profile */
  profileConditions: ProfileCondition[];
  setProfileConditions: (conditions: ProfileCondition[]) => void;
}

const STORAGE_KEY = 'recipe_maker_profile_conditions';

const PatientProfileContext = createContext<PatientProfileContextType | null>(null);

function loadFromStorage(): ProfileCondition[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as ProfileCondition[]) : [];
  } catch {
    return [];
  }
}

/**
 * Provides patient profile state (selected conditions) persisted to localStorage.
 * Any component can read/update profile conditions via usePatientProfile().
 */
export function PatientProfileProvider({ children }: { children: ReactNode }) {
  const [profileConditions, setProfileConditionsState] = useState<ProfileCondition[]>(loadFromStorage);

  const setProfileConditions = useCallback((conditions: ProfileCondition[]) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(conditions));
    setProfileConditionsState(conditions);
  }, []);

  return (
    <PatientProfileContext.Provider value={{ profileConditions, setProfileConditions }}>
      {children}
    </PatientProfileContext.Provider>
  );
}

export function usePatientProfile(): PatientProfileContextType {
  const ctx = useContext(PatientProfileContext);
  if (!ctx) throw new Error('usePatientProfile must be used within <PatientProfileProvider>');
  return ctx;
}
