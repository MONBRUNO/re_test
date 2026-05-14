package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponseDto {
    private final Long id;
    private final String title;
    private final String body;
    private final String route;
    private final boolean read;
    private final LocalDateTime createdAt;

    public NotificationResponseDto(Notification n) {
        this.id = n.getId();
        this.title = n.getTitle();
        this.body = n.getBody();
        this.route = n.getRoute();
        this.read = n.isRead();
        this.createdAt = n.getCreatedAt();
    }
}
