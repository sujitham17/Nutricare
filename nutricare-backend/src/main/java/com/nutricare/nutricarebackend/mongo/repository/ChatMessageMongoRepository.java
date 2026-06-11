package com.nutricare.nutricarebackend.mongo.repository;

import com.nutricare.nutricarebackend.mongo.document.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {

    List<ChatMessageDocument> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Optional<ChatMessageDocument> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
