package com.aijapanese.speaking.manager.service;

import com.aijapanese.speaking.common.dto.UserStatusChangeResponse;
import com.aijapanese.speaking.manager.dto.LearnerSummaryResponse;
import com.aijapanese.speaking.speaking.repository.SpeakingSessionRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.exception.InvalidUserStatusTransitionException;
import com.aijapanese.speaking.user.exception.UserNotFoundException;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class ManagerLearnerService {

    private final UserRepository userRepository;
    private final SpeakingSessionRepository speakingSessionRepository;

    public ManagerLearnerService(UserRepository userRepository, SpeakingSessionRepository speakingSessionRepository) {
        this.userRepository = userRepository;
        this.speakingSessionRepository = speakingSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<LearnerSummaryResponse> getLearners(Long managerUserId, UserStatus status, String keyword) {
        User manager = getManager(managerUserId);
        Long organizationId = manager.getOrganization().getId();
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? "" : keyword.trim();

        List<User> learners = userRepository.search(UserRole.LEARNER, organizationId, status, normalizedKeyword);
        if (learners.isEmpty()) {
            return List.of();
        }

        List<Long> learnerIds = learners.stream().map(User::getId).toList();
        Map<Long, SpeakingSessionRepository.LearnerActivity> activityByUserId = speakingSessionRepository
                .findActivityByUserIds(learnerIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        SpeakingSessionRepository.LearnerActivity::getUserId,
                        Function.identity()
                ));

        return learners.stream()
                .map(learner -> {
                    SpeakingSessionRepository.LearnerActivity activity = activityByUserId.get(learner.getId());
                    long studyCount = activity == null ? 0L : activity.getSessionCount();
                    LocalDateTime lastActivityAt = activity == null ? null : activity.getLastActivityAt();
                    return LearnerSummaryResponse.of(learner, studyCount, lastActivityAt);
                })
                .toList();
    }

    @Transactional
    public UserStatusChangeResponse approve(Long managerUserId, Long learnerId) {
        User learner = getLearnerInManagerOrganization(managerUserId, learnerId);
        applyTransition(learner, UserStatus.PENDING, UserStatus.ACTIVE);
        return response(learner, "학습자 가입을 승인했습니다.");
    }

    @Transactional
    public UserStatusChangeResponse reject(Long managerUserId, Long learnerId) {
        User learner = getLearnerInManagerOrganization(managerUserId, learnerId);
        applyTransition(learner, UserStatus.PENDING, UserStatus.REJECTED);
        return response(learner, "학습자 가입을 거절했습니다.");
    }

    @Transactional
    public UserStatusChangeResponse deactivate(Long managerUserId, Long learnerId) {
        User learner = getLearnerInManagerOrganization(managerUserId, learnerId);
        applyTransition(learner, UserStatus.ACTIVE, UserStatus.INACTIVE);
        return response(learner, "학습자 계정을 비활성화했습니다.");
    }

    @Transactional
    public UserStatusChangeResponse activate(Long managerUserId, Long learnerId) {
        User learner = getLearnerInManagerOrganization(managerUserId, learnerId);
        applyTransition(learner, UserStatus.INACTIVE, UserStatus.ACTIVE);
        return response(learner, "학습자 계정을 활성화했습니다.");
    }

    private User getManager(Long managerUserId) {
        return userRepository.findById(managerUserId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private User getLearnerInManagerOrganization(Long managerUserId, Long learnerId) {
        User manager = getManager(managerUserId);
        return userRepository.findByIdAndOrganization_IdAndRole(learnerId, manager.getOrganization().getId(), UserRole.LEARNER)
                .orElseThrow(() -> new UserNotFoundException("소속 기관에서 학습자를 찾을 수 없습니다."));
    }

    private void applyTransition(User user, UserStatus expected, UserStatus next) {
        if (user.getStatus() != expected) {
            throw new InvalidUserStatusTransitionException(
                    "현재 상태(" + user.getStatus() + ")에서는 해당 작업을 수행할 수 없습니다."
            );
        }
        user.updateStatus(next);
    }

    private UserStatusChangeResponse response(User user, String message) {
        return new UserStatusChangeResponse(user.getId(), user.getStatus(), message);
    }
}
