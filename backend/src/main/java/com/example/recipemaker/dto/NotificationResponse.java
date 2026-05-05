package com.example.recipemaker.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String message;
    private String type;
    private Long orderId;
    private boolean read;
    private LocalDateTime createdAt;
}
