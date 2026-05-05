package com.example.recipemaker.event;

import com.example.recipemaker.model.MealOrder;
import com.example.recipemaker.model.OrderStatus;
import org.springframework.context.ApplicationEvent;

/** Fired after Kitchen/Admin advances an order's status. */
public class OrderStatusChangedEvent extends ApplicationEvent {

    private final MealOrder order;
    private final OrderStatus fromStatus;

    public OrderStatusChangedEvent(Object source, MealOrder order, OrderStatus fromStatus) {
        super(source);
        this.order = order;
        this.fromStatus = fromStatus;
    }

    public MealOrder getOrder() {
        return order;
    }

    public OrderStatus getFromStatus() {
        return fromStatus;
    }
}
