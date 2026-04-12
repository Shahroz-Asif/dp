import { useCallback, useEffect, useState } from 'react';
import { conditionRepository } from '../api/conditions';
import type { PatientCondition } from '../types/api';

export interface UseConditionsResult {
  conditions: PatientCondition[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

export function useConditions(): UseConditionsResult {
  const [conditions, setConditions] = useState<PatientCondition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(() => {
    setLoading(true);
    setError(null);
    conditionRepository
      .getAll()
      .then(setConditions)
      .catch(() => setError('Failed to load conditions.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(fetch, [fetch]);

  return { conditions, loading, error, refresh: fetch };
}
