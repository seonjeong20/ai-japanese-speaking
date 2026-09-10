package com.aijapanese.speaking.conversation.repository;

import com.aijapanese.speaking.conversation.entity.ConversationCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationCorrectionRepository extends JpaRepository<ConversationCorrection, Long> {

    List<ConversationCorrection> findByFeedback_IdOrderByDisplayOrderAsc(Long feedbackId);
}
