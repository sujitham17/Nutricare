package com.nutricare.nutricarebackend.mongo.document;

import com.nutricare.nutricarebackend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notification_logs")
@CompoundIndex(name = "idx_notification_receiver_title_message", def = "{'receiverId': 1, 'title': 1, 'message': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogDocument {

    @Id
    private String id;

    @Indexed
    private Long mysqlLogId;

    @Indexed
    private Long receiverId;

    private Role receiverRole;
    private String receiverPhone;
    private String receiverName;
    private String receiverEmail;
    private String channel;
    private String title;
    private String message;
    private String status;
    private String twilioSid;
    private String errorType;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
