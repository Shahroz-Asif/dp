package com.example.recipemaker.service;

import com.example.recipemaker.dto.NotificationResponse;
import com.example.recipemaker.model.*;
import com.example.recipemaker.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NotificationService — the concrete Observer
 * in the Observer pattern.
 *
 * Rather than going through the full Spring event pipeline
 * (@TransactionalEventListener fires only on AFTER_COMMIT, which never
 * happens inside a @Transactional test), we invoke the observer methods
 * (onOrderPlaced / onOrderStatusAdvanced) directly.  This isolates the
 * observer logic and persistence from the event bus while still running
 * against the real H2 database.
 *
 * Design patterns exercised here:
 *   Observer  — NotificationService implements OrderEventObserver
 *   Builder   — MealOrder.builder(), Notification.builder()
 */
@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepo;
    @Autowired AppUserRepository userRepo;
    @Autowired RecipeRepository recipeRepo;
    @Autowired PatientProfileRepository profileRepo;
    @Autowired MealOrderRepository orderRepo;

    /** Build and persist a minimal MealOrder for patient2 ("Bob Jones"). */
    private MealOrder buildOrderForPatient2() {
        AppUser patientUser = userRepo.findByUsername("patient2").orElseThrow();
        PatientProfile patient = profileRepo.findByUser(patientUser).orElseThrow();
        Recipe recipe = recipeRepo.findAll().stream()
                .filter(r -> r.getName().equals("Grilled Chicken & Broccoli"))
                .findFirst().orElseThrow();

        return orderRepo.save(MealOrder.builder()
                .patient(patient)
                .recipe(recipe)
                .status(OrderStatus.REQUESTED)
                .orderDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void loginAs(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearCtx() {
        SecurityContextHolder.clearContext();
    }

    // ── Observer Pattern ─────────────────────────────────────────────

    @Nested
    class ObserverPatternTests {

        /**
         * Concrete Observer: onOrderPlaced must create ONE notification
         * for every KITCHEN and ADMIN user in the system.
         */
        @Test
        void onOrderPlaced_notifiesKitchenAndAdmin() {
            long beforeKitchen = notificationRepo.countByRecipientUsernameAndReadFalse("kitchen");
            long beforeAdmin   = notificationRepo.countByRecipientUsernameAndReadFalse("admin");

            MealOrder order = buildOrderForPatient2();
            notificationService.onOrderPlaced(order);

            assertEquals(beforeKitchen + 1,
                    notificationRepo.countByRecipientUsernameAndReadFalse("kitchen"),
                    "kitchen should have one new unread notification");
            assertEquals(beforeAdmin + 1,
                    notificationRepo.countByRecipientUsernameAndReadFalse("admin"),
                    "admin should have one new unread notification");
        }

        /**
         * The patient who placed the order must NOT receive an ORDER_PLACED notification.
         */
        @Test
        void onOrderPlaced_doesNotNotifyPatient() {
            long before = notificationRepo.countByRecipientUsernameAndReadFalse("patient2");

            notificationService.onOrderPlaced(buildOrderForPatient2());

            assertEquals(before, notificationRepo.countByRecipientUsernameAndReadFalse("patient2"),
                    "patient2 should not be notified about their own order placement");
        }

        /**
         * Concrete Observer: onOrderStatusAdvanced must notify the patient
         * whose order changed — not kitchen/admin.
         */
        @Test
        void onOrderStatusAdvanced_notifiesPatient() {
            long before = notificationRepo.countByRecipientUsernameAndReadFalse("patient2");

            MealOrder order = buildOrderForPatient2();
            order.setStatus(OrderStatus.PREPARING);
            notificationService.onOrderStatusAdvanced(order, OrderStatus.REQUESTED);

            assertEquals(before + 1,
                    notificationRepo.countByRecipientUsernameAndReadFalse("patient2"),
                    "patient2 should receive a status-update notification");
        }

        /**
         * Status-update notification must NOT be sent to kitchen.
         */
        @Test
        void onOrderStatusAdvanced_doesNotNotifyKitchen() {
            long before = notificationRepo.countByRecipientUsernameAndReadFalse("kitchen");

            MealOrder order = buildOrderForPatient2();
            order.setStatus(OrderStatus.PREPARING);
            notificationService.onOrderStatusAdvanced(order, OrderStatus.REQUESTED);

            assertEquals(before, notificationRepo.countByRecipientUsernameAndReadFalse("kitchen"),
                    "kitchen should not receive a status-update notification");
        }

        /**
         * Verify message content for ORDER_PLACED: must contain the patient's
         * name and the recipe name so kitchen knows what arrived.
         */
        @Test
        void onOrderPlaced_messageContainsPatientAndRecipeName() {
            MealOrder order = buildOrderForPatient2();
            notificationService.onOrderPlaced(order);

            Notification latest = notificationRepo
                    .findByRecipientUsernameOrderByCreatedAtDesc("kitchen")
                    .get(0);

            assertTrue(latest.getMessage().contains("Bob Jones"),
                    "Notification must contain the patient's name");
            assertTrue(latest.getMessage().contains("Grilled Chicken & Broccoli"),
                    "Notification must contain the recipe name");
            assertEquals(NotificationService.TYPE_ORDER_PLACED, latest.getType());
            assertEquals(order.getId(), latest.getOrderId());
            assertFalse(latest.isRead(), "New notification should start as unread");
        }

        /**
         * Verify message content for STATUS_UPDATED: must contain the order ID
         * and the new status text.
         */
        @Test
        void onOrderStatusAdvanced_messageContainsOrderIdAndStatus() {
            MealOrder order = buildOrderForPatient2();
            order.setStatus(OrderStatus.READY);
            notificationService.onOrderStatusAdvanced(order, OrderStatus.PREPARING);

            Notification latest = notificationRepo
                    .findByRecipientUsernameOrderByCreatedAtDesc("patient2")
                    .get(0);

            assertTrue(latest.getMessage().contains(order.getId().toString()),
                    "Message must contain the order ID");
            assertTrue(latest.getMessage().contains("READY"),
                    "Message must contain the new status");
            assertEquals(NotificationService.TYPE_STATUS_UPDATE, latest.getType());
            assertEquals(order.getId(), latest.getOrderId());
        }

        /**
         * Each status transition produces a separate notification —
         * notifications accumulate across the full lifecycle.
         */
        @Test
        void multipleStatusChangesProduceMultipleNotifications() {
            MealOrder order = buildOrderForPatient2();
            long before = notificationRepo.countByRecipientUsernameAndReadFalse("patient2");

            order.setStatus(OrderStatus.PREPARING);
            notificationService.onOrderStatusAdvanced(order, OrderStatus.REQUESTED);

            order.setStatus(OrderStatus.READY);
            notificationService.onOrderStatusAdvanced(order, OrderStatus.PREPARING);

            order.setStatus(OrderStatus.DONE);
            notificationService.onOrderStatusAdvanced(order, OrderStatus.READY);

            assertEquals(before + 3,
                    notificationRepo.countByRecipientUsernameAndReadFalse("patient2"),
                    "Patient should have 3 new unread notifications (one per status transition)");
        }
    }

    // ── REST helpers ─────────────────────────────────────────────────

    @Nested
    class MarkReadTests {

        @Test
        void markRead_setsReadFlagOnNotification() {
            MealOrder order = buildOrderForPatient2();
            notificationService.onOrderPlaced(order); // creates unread notif for "kitchen"

            loginAs("kitchen", "KITCHEN");
            NotificationResponse unread = notificationService.getForCurrentUser().stream()
                    .filter(n -> !n.isRead()).findFirst().orElseThrow();
            assertFalse(unread.isRead());

            NotificationResponse updated = notificationService.markRead(unread.getId());
            assertTrue(updated.isRead());

            // Verify persisted
            Notification persisted = notificationRepo.findById(unread.getId()).orElseThrow();
            assertTrue(persisted.isRead());
        }

        @Test
        void markAllRead_clearsAllUnreadForCurrentUser() {
            MealOrder order = buildOrderForPatient2();
            notificationService.onOrderPlaced(order); // unread for kitchen

            loginAs("kitchen", "KITCHEN");
            notificationService.markAllRead();

            long remaining = notificationRepo.countByRecipientUsernameAndReadFalse("kitchen");
            assertEquals(0, remaining, "No unread notifications should remain after markAllRead");
        }

        @Test
        void markRead_throwsSecurityExceptionForForeignNotification() {
            MealOrder order = buildOrderForPatient2();
            notificationService.onOrderPlaced(order); // belongs to "kitchen"

            Long kitchenNotifId = notificationRepo
                    .findByRecipientUsernameOrderByCreatedAtDesc("kitchen")
                    .get(0).getId();

            loginAs("patient2", "PATIENT"); // different user
            assertThrows(SecurityException.class,
                    () -> notificationService.markRead(kitchenNotifId));
        }

        @Test
        void markRead_throwsNoSuchElementForUnknownId() {
            loginAs("kitchen", "KITCHEN");
            assertThrows(java.util.NoSuchElementException.class,
                    () -> notificationService.markRead(99999L));
        }

        @Test
        void getForCurrentUser_returnsOnlyOwnNotifications() {
            MealOrder order = buildOrderForPatient2();
            notificationService.onOrderPlaced(order); // notifies kitchen AND admin

            // kitchen sees only their own
            loginAs("kitchen", "KITCHEN");
            List<NotificationResponse> kitchenNotifs = notificationService.getForCurrentUser();
            kitchenNotifs.forEach(n -> {
                Notification raw = notificationRepo.findById(n.getId()).orElseThrow();
                assertEquals("kitchen", raw.getRecipientUsername());
            });

            // admin sees only their own
            loginAs("admin", "ADMIN");
            List<NotificationResponse> adminNotifs = notificationService.getForCurrentUser();
            adminNotifs.forEach(n -> {
                Notification raw = notificationRepo.findById(n.getId()).orElseThrow();
                assertEquals("admin", raw.getRecipientUsername());
            });
        }

        @Test
        void getForCurrentUser_returnedNewestFirst() {
            MealOrder order = buildOrderForPatient2();
            notificationService.onOrderPlaced(order);

            MealOrder order2 = buildOrderForPatient2();
            order2.setStatus(OrderStatus.PREPARING);
            notificationService.onOrderStatusAdvanced(order2, OrderStatus.REQUESTED);

            // Force patient2 to have 2 notifications by placing another order
            // (Just reuse the status update path)
            MealOrder order3 = buildOrderForPatient2();
            order3.setStatus(OrderStatus.DONE);
            notificationService.onOrderStatusAdvanced(order3, OrderStatus.READY);

            loginAs("patient2", "PATIENT");
            List<NotificationResponse> notifs = notificationService.getForCurrentUser();

            // Verify descending createdAt order
            for (int i = 0; i < notifs.size() - 1; i++) {
                assertFalse(
                        notifs.get(i).getCreatedAt().isBefore(notifs.get(i + 1).getCreatedAt()),
                        "Notifications must be returned newest first");
            }
        }
    }
}
