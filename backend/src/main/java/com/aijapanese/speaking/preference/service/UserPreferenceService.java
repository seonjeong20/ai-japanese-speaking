package com.aijapanese.speaking.preference.service;

import com.aijapanese.speaking.conversation.entity.Difficulty;
import com.aijapanese.speaking.conversation.entity.SubtitleMode;
import com.aijapanese.speaking.preference.dto.UpdateUserPreferencesRequest;
import com.aijapanese.speaking.preference.dto.UserPreferencesResponse;
import com.aijapanese.speaking.preference.entity.UserPreference;
import com.aijapanese.speaking.preference.repository.UserPreferenceRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.exception.UserNotFoundException;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * 학습 기본값. 이미 만든 세션의 설정(conversation_settings, interview_settings)은 건드리지 않는다.
 */
@Service
public class UserPreferenceService {

    static final Difficulty DEFAULT_DIFFICULTY = Difficulty.INTERMEDIATE;
    static final SubtitleMode DEFAULT_SUBTITLE_MODE = SubtitleMode.JAPANESE_KOREAN;
    static final String SAVED_MESSAGE = "설정이 저장되었습니다.";

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate newTransactionTemplate;

    public UserPreferenceService(
            UserPreferenceRepository userPreferenceRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.newTransactionTemplate = new TransactionTemplate(transactionManager);
        this.newTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 저장한 적이 없으면 INTERMEDIATE, JAPANESE_KOREAN을 돌려주고 행을 만들지 않는다.
     */
    @Transactional(readOnly = true)
    public UserPreferencesResponse get(Long userId) {
        return userPreferenceRepository.findByUser_Id(userId)
                .map(preference -> new UserPreferencesResponse(
                        preference.getDefaultDifficulty(), preference.getDefaultSubtitleMode(), null))
                .orElseGet(() -> new UserPreferencesResponse(DEFAULT_DIFFICULTY, DEFAULT_SUBTITLE_MODE, null));
    }

    /**
     * 사용자당 한 행을 만들거나 고친다.
     * 첫 저장이 동시에 와서 user_id 유일 제약 위반이 나면, 새 트랜잭션에서 먼저 저장된 행을 다시 읽어 이번 값으로 고친다.
     */
    public UserPreferencesResponse update(Long userId, UpdateUserPreferencesRequest request) {
        try {
            return transactionTemplate.execute(status -> createOrUpdate(userId, request));
        } catch (DataIntegrityViolationException ex) {
            return newTransactionTemplate.execute(status -> userPreferenceRepository.findByUser_Id(userId)
                    .map(existing -> updateExisting(existing, request))
                    .orElseThrow(() -> ex));
        }
    }

    private UserPreferencesResponse createOrUpdate(Long userId, UpdateUserPreferencesRequest request) {
        return userPreferenceRepository.findByUser_Id(userId)
                .map(existing -> updateExisting(existing, request))
                .orElseGet(() -> {
                    UserPreference created = userPreferenceRepository.save(new UserPreference(
                            findUser(userId), request.defaultDifficulty(), request.defaultSubtitleMode(),
                            LocalDateTime.now()));
                    return savedResponse(created);
                });
    }

    private UserPreferencesResponse updateExisting(UserPreference existing, UpdateUserPreferencesRequest request) {
        existing.update(request.defaultDifficulty(), request.defaultSubtitleMode(), LocalDateTime.now());
        return savedResponse(existing);
    }

    private static UserPreferencesResponse savedResponse(UserPreference preference) {
        return new UserPreferencesResponse(
                preference.getDefaultDifficulty(), preference.getDefaultSubtitleMode(), SAVED_MESSAGE);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
