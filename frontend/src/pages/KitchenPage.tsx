import { useCallback, useEffect, useState } from 'react';
import { kitchenRepository } from '../api/kitchen';
import type { MealOrderResponse, OrderStatus } from '../types/api';

const STATUS_LABELS: Record<OrderStatus, string> = {
  REQUESTED: 'Requested',
  PREPARING: 'Preparing',
  READY: 'Ready for Pickup',
  DONE: 'Done',
};

const NEXT_ACTION: Partial<Record<OrderStatus, string>> = {
  REQUESTED: 'Start Preparing',
  PREPARING: 'Mark Ready',
  READY: 'Mark Done',
};

const STATUS_CLASSES: Record<OrderStatus, string> = {
  REQUESTED: 'status-requested',
  PREPARING: 'status-preparing',
  READY: 'status-ready',
  DONE: 'status-done',
};

const STATUS_ORDER: OrderStatus[] = ['REQUESTED', 'PREPARING', 'READY'];

export function KitchenPage() {
  const [orders, setOrders] = useState<MealOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [advancingId, setAdvancingId] = useState<number | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    kitchenRepository
      .getOrders()
      .then(setOrders)
      .catch(() => setError('Failed to load kitchen orders.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  const handleAdvance = async (id: number) => {
    setAdvancingId(id);
    try {
      const updated = await kitchenRepository.advanceStatus(id);
      setOrders((prev) =>
        prev.map((o) => (o.id === id ? updated : o)).filter((o) => o.status !== 'DONE')
      );
    } catch {
      setError('Failed to advance order status.');
    } finally {
      setAdvancingId(null);
    }
  };

  if (loading) return <p className="page-loading">Loading kitchen orders…</p>;
  if (error) return <p className="error-msg">{error}</p>;

  const grouped = STATUS_ORDER.map((status) => ({
    status,
    orders: orders.filter((o) => o.status === status),
  }));

  return (
    <div className="page">
      <div className="page-header">
        <h2>Kitchen</h2>
        <button className="btn btn-secondary" onClick={load}>
          Refresh
        </button>
      </div>

      {orders.length === 0 ? (
        <p className="empty-state">No active orders at the moment.</p>
      ) : (
        <div className="kitchen-columns">
          {grouped.map(({ status, orders: statusOrders }) => (
            <div key={status} className="kitchen-column">
              <h3 className="kitchen-column-title">
                <span className={`status-badge ${STATUS_CLASSES[status]}`}>
                  {STATUS_LABELS[status]}
                </span>
                <span className="kitchen-column-count">{statusOrders.length}</span>
              </h3>

              {statusOrders.length === 0 ? (
                <p className="kitchen-empty">Empty</p>
              ) : (
                statusOrders.map((order) => (
                  <div key={order.id} className="order-card kitchen-order-card">
                    <h4 className="order-recipe-name">{order.recipeName}</h4>
                    <p className="order-patient">Patient: {order.patientName}</p>
                    <div className="order-meta">
                      <span className={`meal-course-badge course-${order.mealCourse.toLowerCase()}`}>
                        {order.mealCourse}
                      </span>
                      <span className={`meal-type-badge type-${order.mealType.toLowerCase()}`}>
                        {order.mealType}
                      </span>
                    </div>
                    {NEXT_ACTION[order.status] && (
                      <button
                        className="btn btn-primary btn-sm kitchen-advance-btn"
                        disabled={advancingId === order.id}
                        onClick={() => handleAdvance(order.id)}
                      >
                        {advancingId === order.id ? '…' : NEXT_ACTION[order.status]}
                      </button>
                    )}
                  </div>
                ))
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
