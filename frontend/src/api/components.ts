import client from './client';
import type { RecipeComponent } from '../types/api';

/** New-component payload — id is assigned by the database */
export interface CreateComponentBody {
  type: 'MODIFIABLE' | 'NON_MODIFIABLE';
  name: string;
  description: string;
  incompatibleConditions: [];
}

/**
 * Repository Pattern — abstracts HTTP calls for the RecipeComponent resource.
 * The backend's ComponentController works directly with the JPA repository and
 * returns polymorphic JSON that includes the Jackson "type" discriminator.
 */
export const componentRepository = {
  getAll(): Promise<RecipeComponent[]> {
    return client.get<RecipeComponent[]>('/components').then((r) => r.data);
  },

  create(body: CreateComponentBody): Promise<RecipeComponent> {
    return client.post<RecipeComponent>('/components', body).then((r) => r.data);
  },

  delete(id: number): Promise<void> {
    return client.delete(`/components/${id}`).then(() => undefined);
  },
};
