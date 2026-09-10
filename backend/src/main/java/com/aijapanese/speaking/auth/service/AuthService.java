package com.aijapanese.speaking.auth.service;

import com.aijapanese.speaking.auth.dto.LoginRequest;
import com.aijapanese.speaking.auth.dto.LoginResponse;
import com.aijapanese.speaking.auth.dto.SignupRequest;
import com.aijapanese.speaking.auth.dto.SignupResponse;
import com.aijapanese.speaking.auth.dto.SignupRole;
import com.aijapanese.speaking.auth.dto.UserResponse;
import com.aijapanese.speaking.auth.exception.AccountNotActiveException;
import com.aijapanese.speaking.auth.exception.EmailAlreadyExistsException;
import com.aijapanese.speaking.auth.exception.InvalidCredentialsException;
import com.aijapanese.speaking.auth.exception.OrganizationNotAvailableException;
import com.aijapanese.speaking.auth.exception.UnsupportedSignupRoleException;
import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.organization.entity.Organization;
import com.aijapanese.speaking.organization.entity.OrganizationStatus;
import com.aijapanese.speaking.organization.repository.OrganizationRepository;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import com.aijapanese.speaking.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (request.role() != SignupRole.LEARNER) {
            throw new UnsupportedSignupRoleException("현재는 LEARNER 가입만 지원합니다.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("이미 가입된 이메일입니다.");
        }

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new OrganizationNotAvailableException("선택한 기관을 찾을 수 없습니다."));

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new OrganizationNotAvailableException("선택한 기관은 현재 가입할 수 없는 상태입니다.");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                organization,
                request.name(),
                request.email(),
                passwordHash,
                request.department(),
                UserRole.LEARNER,
                UserStatus.PENDING,
                LocalDateTime.now()
        );

        User saved = userRepository.save(user);

        return new SignupResponse(
                saved.getId(),
                SignupRole.LEARNER,
                saved.getStatus(),
                "가입 신청이 완료되었습니다. 소속 기관 매니저 승인 후 이용할 수 있습니다."
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        switch (user.getStatus()) {
            case PENDING -> throw new AccountNotActiveException(UserStatus.PENDING, "현재 가입 승인 대기 중입니다.");
            case REJECTED -> throw new AccountNotActiveException(UserStatus.REJECTED, "가입이 거절되었습니다.");
            case INACTIVE -> throw new AccountNotActiveException(UserStatus.INACTIVE, "비활성화된 계정입니다.");
            case ACTIVE -> { }
        }

        user.updateLastLoginAt(LocalDateTime.now());

        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getRole());

        return new LoginResponse(accessToken, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("유효하지 않은 인증 정보입니다."));
        return UserResponse.from(user);
    }
}
