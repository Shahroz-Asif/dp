package com.example.recipemaker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username of the user who should receive this notification */
    @Column(nullable = false)
    private String recipientUsername;

    @Column(nullable = false)
    private String message;

    /** Discriminates the event type: ORDER_PLACED or STATUS_UPDATED */
    @Column(nullable = false)
    private String type;

    /** The order this notification is about */
    @Column(nullable = false)
    private Long orderId;

    @Builder.Default
    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
