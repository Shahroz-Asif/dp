import { useCallback, useEffect, useState } from 'react';
import { recipeRepository } from '../api/recipes';
import type { RecipeResponse } from '../types/api';

export interface UseRecipesResult {
  recipes: RecipeResponse[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

/** Fetches all recipes on mount and exposes a refresh callback */
export function useRecipes(): UseRecipesResult {
  const [recipes, setRecipes] = useState<RecipeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(() => {
    setLoading(true);
    setError(null);
    recipeRepository
      .getAll()
      .then(setRecipes)
      .catch(() => setError('Failed to load recipes. Is the backend running?'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(fetch, [fetch]);

  return { recipes, loading, error, refresh: fetch };
}
