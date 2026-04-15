import client from './client';
import type { MealOrderRequest, MealOrderResponse } from '../types/api';

export const orderRepository = {
  placeOrder(body: MealOrderRequest): Promise<MealOrderResponse> {
    return client.post<MealOrderResponse>('/orders', body).then((r) => r.data);
  },

  getActiveOrders(): Promise<MealOrderResponse[]> {
    return client.get<MealOrderResponse[]>('/orders/active').then((r) => r.data);
  },

  getOrderHistory(): Promise<MealOrderResponse[]> {
    return client.get<MealOrderResponse[]>('/orders/history').then((r) => r.data);
  },
};
