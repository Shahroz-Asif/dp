import { useEffect, useState } from 'react';
import { orderRepository } from '../api/orders';
import type { MealOrderResponse, OrderStatus } from '../types/api';

const STATUS_LABELS: Record<OrderStatus, string> = {
  REQUESTED: 'Requested',
  PREPARING: 'Preparing',
  READY: 'Ready for Pickup',
  DONE: 'Done',
};

const STATUS_CLASSES: Record<OrderStatus, string> = {
  REQUESTED: 'status-requested',
  PREPARING: 'status-preparing',
  READY: 'status-ready',
  DONE: 'status-done',
};

export function ActiveOrdersPage() {
  const [orders, setOrders] = useState<MealOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    orderRepository
      .getActiveOrders()
      .then(setOrders)
      .catch(() => setError('Failed to load orders.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="page-loading">Loading orders…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>My Active Orders</h2>
      </div>

      {orders.length === 0 ? (
        <p className="empty-state">No active orders. Browse the menu to place an order.</p>
      ) : (
        <div className="order-list">
          {orders.map((order) => (
            <div key={order.id} className="order-card">
              <div className="order-card-header">
                <div>
                  <h3 className="order-recipe-name">{order.recipeName}</h3>
                  <div className="order-meta">
                    <span className={`meal-course-badge course-${order.mealCourse.toLowerCase()}`}>
                      {order.mealCourse}
                    </span>
                    <span className={`meal-type-badge type-${order.mealType.toLowerCase()}`}>
                      {order.mealType}
                    </span>
                  </div>
                </div>
                <span className={`status-badge ${STATUS_CLASSES[order.status]}`}>
                  {STATUS_LABELS[order.status]}
                </span>
              </div>
              <p className="order-date">Ordered: {order.orderDate}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
