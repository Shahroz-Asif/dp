package com.example.recipemaker.observer;

import com.example.recipemaker.model.MealOrder;
import com.example.recipemaker.model.OrderStatus;

/**
 * Observer interface (Observer Pattern).
 *
 * Implementors react to order lifecycle events published by MealOrderService.
 * New observers can be wired in without touching the subject.
 */
public interface OrderEventObserver {

    /** Called after a patient successfully places a new order. */
    void onOrderPlaced(MealOrder order);

    /**
     * Called after Kitchen/Admin advances an order's status.
     *
     * @param order       the updated order (status is already the new value)
     * @param fromStatus  the status before the change
     */
    void onOrderStatusAdvanced(MealOrder order, OrderStatus fromStatus);
}
