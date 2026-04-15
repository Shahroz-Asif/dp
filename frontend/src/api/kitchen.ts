import client from './client';
import type { MealOrderResponse } from '../types/api';

export const kitchenRepository = {
  getOrders(): Promise<MealOrderResponse[]> {
    return client.get<MealOrderResponse[]>('/kitchen/orders').then((r) => r.data);
  },

  advanceStatus(id: number): Promise<MealOrderResponse> {
    return client.put<MealOrderResponse>(`/kitchen/orders/${id}/advance`).then((r) => r.data);
  },
};
