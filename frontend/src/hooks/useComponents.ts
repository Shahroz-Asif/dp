import { useCallback, useEffect, useState } from 'react';
import { componentRepository } from '../api/components';
import type { RecipeComponent } from '../types/api';

export interface UseComponentsResult {
  components: RecipeComponent[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

export function useComponents(): UseComponentsResult {
  const [components, setComponents] = useState<RecipeComponent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(() => {
    setLoading(true);
    setError(null);
    componentRepository
      .getAll()
      .then(setComponents)
      .catch(() => setError('Failed to load components.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(fetch, [fetch]);

  return { components, loading, error, refresh: fetch };
}
