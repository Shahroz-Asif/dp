import client from './client';
import type { NotificationResponse } from '../types/api';

export const notificationRepository = {
  getAll(): Promise<NotificationResponse[]> {
    return client.get<NotificationResponse[]>('/notifications').then((r) => r.data);
  },

  markRead(id: number): Promise<NotificationResponse> {
    return client.put<NotificationResponse>(`/notifications/${id}/read`).then((r) => r.data);
  },

  markAllRead(): Promise<void> {
    return client.put('/notifications/read-all').then(() => undefined);
  },
};
