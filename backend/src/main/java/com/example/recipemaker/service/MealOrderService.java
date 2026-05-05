package com.example.recipemaker.service;

import com.example.recipemaker.dto.ComponentResponse;
import com.example.recipemaker.dto.MealOrderRequest;
import com.example.recipemaker.dto.MealOrderResponse;
import com.example.recipemaker.event.OrderPlacedEvent;
import com.example.recipemaker.event.OrderStatusChangedEvent;
import com.example.recipemaker.model.*;
import com.example.recipemaker.repository.AppUserRepository;
import com.example.recipemaker.repository.MealOrderRepository;
import com.example.recipemaker.repository.PatientProfileRepository;
import com.example.recipemaker.repository.RecipeComponentRepository;
import com.example.recipemaker.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MealOrderService {

    private final MealOrderRepository orderRepository;
    private final PatientProfileRepository profileRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeComponentRepository componentRepository;
    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        Set<RecipeComponent> allowedModifiable = recipe.getModifiableComponents();
        Set<PatientCondition> patientConditions = patient.getConditions();
        Set<RecipeComponent> selectedComponents = new HashSet<>();

        for (Long compId : request.getSelectedComponentIds()) {
            RecipeComponent comp = componentRepository.findById(compId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Component not found: " + compId));
            if (!comp.isModifiable()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Component '" + comp.getName() + "' is not a modifiable component.");
            }
            if (!allowedModifiable.contains(comp)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Component '" + comp.getName() + "' does not belong to this recipe.");
            }
            for (PatientCondition cond : comp.getIncompatibleConditions()) {
                if (patientConditions.contains(cond)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Component '" + comp.getName() + "' is incompatible with your condition: " + cond.getName());
                }
            }
            selectedComponents.add(comp);
        }

        MealOrder order = MealOrder.builder()
                .patient(patient)
                .recipe(recipe)
                .selectedComponents(selectedComponents)
                .status(OrderStatus.REQUESTED)
                .orderDate(today)
                .createdAt(LocalDateTime.now())
                .build();

        MealOrder saved = orderRepository.save(order);
        // Publish event — NotificationService (Observer) will fire AFTER this transaction commits
        eventPublisher.publishEvent(new OrderPlacedEvent(this, saved));
        return toResponse(saved);
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

        OrderStatus fromStatus = order.getStatus();
        OrderStatus next = switch (fromStatus) {
            case REQUESTED -> OrderStatus.PREPARING;
            case PREPARING -> OrderStatus.READY;
            case READY -> OrderStatus.DONE;
            case DONE -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is already done");
        };

        order.setStatus(next);
        MealOrder saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(this, saved, fromStatus));
        return toResponse(saved);
    }

    public MealOrderResponse toResponse(MealOrder order) {
        Recipe recipe = order.getRecipe();
        PatientProfile patient = order.getPatient();

        List<ComponentResponse> selectedComponents = order.getSelectedComponents().stream()
                .map(c -> ComponentResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .modifiable(c.isModifiable())
                        .incompatibleConditionNames(c.getIncompatibleConditions().stream()
                                .map(PatientCondition::getName)
                                .collect(Collectors.toSet()))
                        .build())
                .collect(Collectors.toList());

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
                .selectedComponents(selectedComponents)
                .build();
    }
}
