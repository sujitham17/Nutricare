package com.nutricare.nutricarebackend.mongo.repository;

import com.nutricare.nutricarebackend.mongo.document.NotificationLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationLogMongoRepository extends MongoRepository<NotificationLogDocument, String> {

    List<NotificationLogDocument> findAllByOrderByCreatedAtDesc();

    List<NotificationLogDocument> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    List<NotificationLogDocument> findByChannelOrderByCreatedAtDesc(String channel);

    List<NotificationLogDocument> findByStatusOrderByCreatedAtDesc(String status);

    List<NotificationLogDocument> findByReceiverIdAndChannelOrderByCreatedAtDesc(Long receiverId, String channel);

    List<NotificationLogDocument> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, String status);

    List<NotificationLogDocument> findByChannelAndStatusOrderByCreatedAtDesc(String channel, String status);

    List<NotificationLogDocument> findByReceiverIdAndChannelAndStatusOrderByCreatedAtDesc(Long receiverId, String channel, String status);

    boolean existsByReceiverIdAndTitleAndMessage(Long receiverId, String title, String message);
}
