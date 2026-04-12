import client from './client';
import type { PatientCondition } from '../types/api';

/**
 * Repository Pattern — abstracts HTTP calls for the PatientCondition resource.
 */
export const conditionRepository = {
  getAll(): Promise<PatientCondition[]> {
    return client.get<PatientCondition[]>('/conditions').then((r) => r.data);
  },

  create(body: Omit<PatientCondition, 'id'>): Promise<PatientCondition> {
    return client.post<PatientCondition>('/conditions', body).then((r) => r.data);
  },

  delete(id: number): Promise<void> {
    return client.delete(`/conditions/${id}`).then(() => undefined);
  },
};
