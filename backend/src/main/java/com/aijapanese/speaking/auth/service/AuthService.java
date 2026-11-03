package com.aijapanese.speaking.auth.service;

import com.aijapanese.speaking.auth.dto.UserResponse;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.exception.UserNotFoundException;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }
}
