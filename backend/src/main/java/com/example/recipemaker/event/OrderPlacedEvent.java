package com.example.recipemaker.event;

import com.example.recipemaker.model.MealOrder;
import org.springframework.context.ApplicationEvent;

/** Fired after a patient successfully places a new meal order. */
public class OrderPlacedEvent extends ApplicationEvent {

    private final MealOrder order;

    public OrderPlacedEvent(Object source, MealOrder order) {
        super(source);
        this.order = order;
    }

    public MealOrder getOrder() {
        return order;
    }
}
