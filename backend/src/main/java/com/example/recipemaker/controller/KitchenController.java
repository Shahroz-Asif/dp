package com.example.recipemaker.controller;

import com.example.recipemaker.dto.MealOrderResponse;
import com.example.recipemaker.service.MealOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
@Tag(name = "Kitchen", description = "Kitchen order management")
public class KitchenController {

    private final MealOrderService mealOrderService;

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('KITCHEN','ADMIN')")
    @Operation(summary = "Get all active orders (Kitchen/Admin only)")
    public List<MealOrderResponse> getActiveOrders() {
        return mealOrderService.getAllActiveOrders();
    }

    @PutMapping("/orders/{id}/advance")
    @PreAuthorize("hasAnyRole('KITCHEN','ADMIN')")
    @Operation(summary = "Advance order status: REQUESTED→PREPARING→READY→DONE (Kitchen/Admin only)")
    public MealOrderResponse advanceStatus(@PathVariable Long id) {
        return mealOrderService.advanceOrderStatus(id);
    }
}
