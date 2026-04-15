import client from './client';
import type { PatientProfileResponse } from '../types/api';

export const patientRepository = {
  getMyProfile(): Promise<PatientProfileResponse> {
    return client.get<PatientProfileResponse>('/patients/me').then((r) => r.data);
  },
};
