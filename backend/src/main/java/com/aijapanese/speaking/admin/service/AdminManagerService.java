package com.aijapanese.speaking.admin.service;

import com.aijapanese.speaking.admin.dto.ManagerSummaryResponse;
import com.aijapanese.speaking.common.dto.UserStatusChangeResponse;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.exception.InvalidUserStatusTransitionException;
import com.aijapanese.speaking.user.exception.UserNotFoundException;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminManagerService {

    private final UserRepository userRepository;

    public AdminManagerService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ManagerSummaryResponse> getManagers(UserStatus status, Long organizationId) {
        return userRepository.search(UserRole.MANAGER, organizationId, status, "").stream()
                .map(ManagerSummaryResponse::from)
                .toList();
    }

    @Transactional
    public UserStatusChangeResponse approve(Long managerId) {
        User manager = getManager(managerId);
        applyTransition(manager, UserStatus.PENDING, UserStatus.ACTIVE);
        return response(manager, "담당자 가입을 승인했습니다.");
    }

    @Transactional
    public UserStatusChangeResponse reject(Long managerId) {
        User manager = getManager(managerId);
        applyTransition(manager, UserStatus.PENDING, UserStatus.REJECTED);
        return response(manager, "담당자 가입을 거절했습니다.");
    }

    @Transactional
    public UserStatusChangeResponse deactivate(Long managerId) {
        User manager = getManager(managerId);
        applyTransition(manager, UserStatus.ACTIVE, UserStatus.INACTIVE);
        return response(manager, "담당자 계정을 비활성화했습니다.");
    }

    @Transactional
    public UserStatusChangeResponse activate(Long managerId) {
        User manager = getManager(managerId);
        applyTransition(manager, UserStatus.INACTIVE, UserStatus.ACTIVE);
        return response(manager, "담당자 계정을 활성화했습니다.");
    }

    private User getManager(Long managerId) {
        return userRepository.findByIdAndRole(managerId, UserRole.MANAGER)
                .orElseThrow(() -> new UserNotFoundException("담당자를 찾을 수 없습니다."));
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
