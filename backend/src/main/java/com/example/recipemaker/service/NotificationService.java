package com.example.recipemaker.service;

import com.example.recipemaker.dto.NotificationResponse;
import com.example.recipemaker.event.OrderPlacedEvent;
import com.example.recipemaker.event.OrderStatusChangedEvent;
import com.example.recipemaker.model.MealOrder;
import com.example.recipemaker.model.Notification;
import com.example.recipemaker.model.OrderStatus;
import com.example.recipemaker.observer.OrderEventObserver;
import com.example.recipemaker.repository.AppUserRepository;
import com.example.recipemaker.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Concrete Observer -- implements OrderEventObserver via Spring's
 * transactional event system.
 *
 * MealOrderService (Subject) publishes OrderPlacedEvent / OrderStatusChangedEvent
 * through Spring's ApplicationEventPublisher.  These listeners fire AFTER the
 * publishing transaction commits, guaranteeing the order row is visible before
 * notifications are sent.
 *
 * Each listener opens a fresh transaction (REQUIRES_NEW) for notification
 * persistence.  Users connected over WebSocket receive the push immediately;
 * offline users receive persisted rows via GET /api/notifications on next login.
 */
@Service
@RequiredArgsConstructor
public class NotificationService implements OrderEventObserver {

    public static final String TYPE_ORDER_PLACED  = "ORDER_PLACED";
    public static final String TYPE_STATUS_UPDATE = "STATUS_UPDATED";

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // -----------------------------------------------------------------------
    // Spring event bridges -> Observer methods
    // AFTER_COMMIT: order is durable before we notify.
    // REQUIRES_NEW: fresh transaction for notification rows.
    // -----------------------------------------------------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        onOrderPlaced(event.getOrder());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderStatusChangedEvent(OrderStatusChangedEvent event) {
        onOrderStatusAdvanced(event.getOrder(), event.getFromStatus());
    }

    // -----------------------------------------------------------------------
    // OrderEventObserver -- concrete logic
    // -----------------------------------------------------------------------

    /** Notify every KITCHEN and ADMIN user that a new order arrived. */
    @Override
    public void onOrderPlaced(MealOrder order) {
        String msg = String.format("New order #%d from %s: %s",
                order.getId(),
                order.getPatient().getName(),
                order.getRecipe().getName());

        userRepository.findAll().stream()
                .filter(u -> "KITCHEN".equals(u.getRole()) || "ADMIN".equals(u.getRole()))
                .forEach(u -> saveAndPush(u.getUsername(), msg, TYPE_ORDER_PLACED, order.getId()));
    }

    /** Notify the patient that their order status changed. */
    @Override
    public void onOrderStatusAdvanced(MealOrder order, OrderStatus fromStatus) {
        String msg = String.format("Your order #%d (%s) is now %s",
                order.getId(),
                order.getRecipe().getName(),
                order.getStatus().name());

        String patientUsername = order.getPatient().getUser().getUsername();
        saveAndPush(patientUsername, msg, TYPE_STATUS_UPDATE, order.getId());
    }

    // -----------------------------------------------------------------------
    // REST helpers (used by NotificationController)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationResponse> getForCurrentUser() {
        String username = currentUsername();
        return notificationRepository
                .findByRecipientUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found: " + id));
        if (!n.getRecipientUsername().equals(currentUsername())) {
            throw new SecurityException("Not your notification");
        }
        n.setRead(true);
        return toResponse(notificationRepository.save(n));
    }

    @Transactional
    public void markAllRead() {
        String username = currentUsername();
        List<Notification> unread = notificationRepository
                .findByRecipientUsernameOrderByCreatedAtDesc(username)
                .stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void saveAndPush(String username, String message, String type, Long orderId) {
        Notification saved = notificationRepository.save(Notification.builder()
                .recipientUsername(username)
                .message(message)
                .type(type)
                .orderId(orderId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());

        // Real-time push — silently no-ops if user has no active STOMP session
        // Wrapped in try-catch so a delivery failure never rolls back the persisted row.
        try {
            messagingTemplate.convertAndSendToUser(
                    username, "/queue/notifications", toResponse(saved));
        } catch (Exception ignored) {
            // User not connected; notification is already persisted for retrieval on next login.
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .orderId(n.getOrderId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
