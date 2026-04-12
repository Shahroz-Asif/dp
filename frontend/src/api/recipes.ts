import client from './client';
import type { RecipeRequest, RecipeResponse } from '../types/api';

/**
 * Repository Pattern — abstracts all HTTP calls for the Recipe resource.
 * Pages and hooks only call these methods; they never touch Axios directly.
 */
export const recipeRepository = {
  getAll(): Promise<RecipeResponse[]> {
    return client.get<RecipeResponse[]>('/recipes').then((r) => r.data);
  },

  getById(id: number): Promise<RecipeResponse> {
    return client.get<RecipeResponse>(`/recipes/${id}`).then((r) => r.data);
  },

  create(body: RecipeRequest): Promise<RecipeResponse> {
    return client.post<RecipeResponse>('/recipes', body).then((r) => r.data);
  },

  update(id: number, body: RecipeRequest): Promise<RecipeResponse> {
    return client.put<RecipeResponse>(`/recipes/${id}`, body).then((r) => r.data);
  },

  delete(id: number): Promise<void> {
    return client.delete(`/recipes/${id}`).then(() => undefined);
  },

  search(params: {
    name?: string;
    componentName?: string;
    compatibleConditionIds?: number[];
  }): Promise<RecipeResponse[]> {
    return client
      .get<RecipeResponse[]>('/recipes/search', { params })
      .then((r) => r.data);
  },
};
