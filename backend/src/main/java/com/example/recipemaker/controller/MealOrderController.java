package com.example.recipemaker.controller;

import com.example.recipemaker.dto.MealOrderRequest;
import com.example.recipemaker.dto.MealOrderResponse;
import com.example.recipemaker.service.MealOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Meal Orders", description = "Patient meal ordering")
public class MealOrderController {

    private final MealOrderService mealOrderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Place a meal order (Patient only)")
    public MealOrderResponse placeOrder(@RequestBody MealOrderRequest request) {
        return mealOrderService.placeOrder(request);
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get current patient's active orders")
    public List<MealOrderResponse> getActiveOrders() {
        return mealOrderService.getActiveOrdersForCurrentPatient();
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get current patient's order history (completed orders)")
    public List<MealOrderResponse> getOrderHistory() {
        return mealOrderService.getOrderHistoryForCurrentPatient();
    }
}
