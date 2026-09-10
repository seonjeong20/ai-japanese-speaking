package com.aijapanese.speaking.conversation.repository;

import com.aijapanese.speaking.conversation.entity.ConversationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationFeedbackRepository extends JpaRepository<ConversationFeedback, Long> {

    Optional<ConversationFeedback> findBySession_Id(Long sessionId);
}
