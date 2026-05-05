package com.example.recipemaker.controller;

import com.example.recipemaker.dto.NotificationResponse;
import com.example.recipemaker.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification management")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for the current user (newest first)")
    public List<NotificationResponse> getNotifications() {
        return notificationService.getForCurrentUser();
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications for the current user as read")
    public void markAllRead() {
        notificationService.markAllRead();
    }
}
