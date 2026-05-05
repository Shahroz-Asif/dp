package com.example.recipemaker.repository;

import com.example.recipemaker.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** All notifications for a user, newest first */
    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    /** Unread count used to power the badge */
    long countByRecipientUsernameAndReadFalse(String recipientUsername);
}
