import client from './client';
import type { PatientProfileResponse } from '../types/api';

export const doctorRepository = {
  getMyPatients(): Promise<PatientProfileResponse[]> {
    return client.get<PatientProfileResponse[]>('/doctor/patients').then((r) => r.data);
  },

  assignCondition(patientId: number, conditionId: number): Promise<void> {
    return client
      .post(`/doctor/patients/${patientId}/conditions/${conditionId}`)
      .then(() => undefined);
  },

  removeCondition(patientId: number, conditionId: number): Promise<void> {
    return client
      .delete(`/doctor/patients/${patientId}/conditions/${conditionId}`)
      .then(() => undefined);
  },
};
