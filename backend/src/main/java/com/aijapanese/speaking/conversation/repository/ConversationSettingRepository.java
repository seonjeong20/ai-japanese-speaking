package com.aijapanese.speaking.conversation.repository;

import com.aijapanese.speaking.conversation.entity.ConversationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationSettingRepository extends JpaRepository<ConversationSetting, Long> {

    Optional<ConversationSetting> findBySession_Id(Long sessionId);
}
