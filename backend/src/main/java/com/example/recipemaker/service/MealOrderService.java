package com.example.recipemaker.service;

import com.example.recipemaker.dto.MealOrderRequest;
import com.example.recipemaker.dto.MealOrderResponse;
import com.example.recipemaker.model.*;
import com.example.recipemaker.repository.AppUserRepository;
import com.example.recipemaker.repository.MealOrderRepository;
import com.example.recipemaker.repository.PatientProfileRepository;
import com.example.recipemaker.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MealOrderService {

    private final MealOrderRepository orderRepository;
    private final PatientProfileRepository profileRepository;
    private final RecipeRepository recipeRepository;
    private final AppUserRepository userRepository;

    private PatientProfile getCurrentPatientProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return profileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No patient profile found. Contact your doctor."));
    }

    public MealOrderResponse placeOrder(MealOrderRequest request) {
        PatientProfile patient = getCurrentPatientProfile();
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new NoSuchElementException("Recipe not found"));

        LocalDate today = LocalDate.now();
        MealCourse course = recipe.getMealCourse();
        MealType mealType = recipe.getMealType();

        if (course == null || mealType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This recipe does not have a meal course/type assigned.");
        }

        long existing = orderRepository.countByPatientAndOrderDateAndRecipe_MealCourseAndRecipe_MealType(
                patient, today, course, mealType);
        if (existing >= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already ordered a " + mealType.name().toLowerCase() +
                            " meal for " + course.name().toLowerCase() + " today.");
        }

        MealOrder order = MealOrder.builder()
                .patient(patient)
                .recipe(recipe)
                .status(OrderStatus.REQUESTED)
                .orderDate(today)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(orderRepository.save(order));
    }

    public List<MealOrderResponse> getActiveOrdersForCurrentPatient() {
        PatientProfile patient = getCurrentPatientProfile();
        return orderRepository.findByPatientAndStatusNotOrderByCreatedAtDesc(patient, OrderStatus.DONE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MealOrderResponse> getOrderHistoryForCurrentPatient() {
        PatientProfile patient = getCurrentPatientProfile();
        return orderRepository.findByPatientAndStatusOrderByCreatedAtDesc(patient, OrderStatus.DONE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MealOrderResponse> getAllActiveOrders() {
        return orderRepository.findByStatusNotOrderByCreatedAtDesc(OrderStatus.DONE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MealOrderResponse advanceOrderStatus(Long orderId) {
        MealOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        OrderStatus next = switch (order.getStatus()) {
            case REQUESTED -> OrderStatus.PREPARING;
            case PREPARING -> OrderStatus.READY;
            case READY -> OrderStatus.DONE;
            case DONE -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is already done");
        };

        order.setStatus(next);
        return toResponse(orderRepository.save(order));
    }

    public MealOrderResponse toResponse(MealOrder order) {
        Recipe recipe = order.getRecipe();
        PatientProfile patient = order.getPatient();
        return MealOrderResponse.builder()
                .id(order.getId())
                .patientProfileId(patient.getId())
                .patientName(patient.getName())
                .recipeId(recipe.getId())
                .recipeName(recipe.getName())
                .mealCourse(recipe.getMealCourse())
                .mealType(recipe.getMealType())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
