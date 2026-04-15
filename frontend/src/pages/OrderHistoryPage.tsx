import { useEffect, useState } from 'react';
import { orderRepository } from '../api/orders';
import type { MealOrderResponse } from '../types/api';

export function OrderHistoryPage() {
  const [orders, setOrders] = useState<MealOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    orderRepository
      .getOrderHistory()
      .then(setOrders)
      .catch(() => setError('Failed to load order history.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="page-loading">Loading history…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Order History</h2>
      </div>

      {orders.length === 0 ? (
        <p className="empty-state">No completed orders yet.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Recipe</th>
              <th>Course</th>
              <th>Type</th>
              <th>Date</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id}>
                <td>{order.recipeName}</td>
                <td>
                  <span className={`meal-course-badge course-${order.mealCourse.toLowerCase()}`}>
                    {order.mealCourse}
                  </span>
                </td>
                <td>
                  <span className={`meal-type-badge type-${order.mealType.toLowerCase()}`}>
                    {order.mealType}
                  </span>
                </td>
                <td>{order.orderDate}</td>
                <td>
                  <span className="status-badge status-done">Done</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
