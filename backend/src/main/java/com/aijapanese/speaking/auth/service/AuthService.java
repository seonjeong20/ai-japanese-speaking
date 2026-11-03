package com.aijapanese.speaking.auth.service;

import com.aijapanese.speaking.auth.dto.LoginResponse;
import com.aijapanese.speaking.auth.dto.OAuthLoginRequest;
import com.aijapanese.speaking.auth.dto.UserResponse;
import com.aijapanese.speaking.auth.exception.EmailAlreadyRegisteredException;
import com.aijapanese.speaking.auth.exception.EmailNotVerifiedException;
import com.aijapanese.speaking.auth.exception.InvalidAuthorizationCodeException;
import com.aijapanese.speaking.auth.exception.OAuthProviderException;
import com.aijapanese.speaking.auth.oauth.SocialProfile;
import com.aijapanese.speaking.auth.oauth.SocialProfileClient;
import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.user.entity.OAuthProvider;
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
import java.util.Optional;

@Service
public class AuthService {

    /** ERD users.name 길이 */
    private static final int MAX_NAME_LENGTH = 50;

    private final UserRepository userRepository;
    private final SocialProfileClient socialProfileClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate newTransactionTemplate;

    public AuthService(
            UserRepository userRepository,
            SocialProfileClient socialProfileClient,
            JwtTokenProvider jwtTokenProvider,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.socialProfileClient = socialProfileClient;
        this.jwtTokenProvider = jwtTokenProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.newTransactionTemplate = new TransactionTemplate(transactionManager);
        this.newTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 소셜 로그인(가입 겸 로그인). 흐름은 docs/specs/account.md 1장.
     * 제공자와의 통신은 DB 트랜잭션 밖에서 하고, 계정 조회·저장만 한 트랜잭션으로 묶는다.
     */
    public LoginResponse oauthLogin(OAuthProvider provider, OAuthLoginRequest request) {
        if (request == null || isBlank(request.code()) || isBlank(request.redirectUri())) {
            throw new InvalidAuthorizationCodeException();
        }

        SocialProfile profile = socialProfileClient.fetch(provider, request.code(), request.redirectUri());
        if (isBlank(profile.providerUserId())) {
            // 제공자 응답에 고유 ID가 없다. 제공자 쪽 문제로 본다.
            throw new OAuthProviderException(null);
        }

        try {
            return transactionTemplate.execute(status -> loginOrSignup(provider, profile));
        } catch (DataIntegrityViolationException ex) {
            // 조회와 저장 사이에 다른 요청이 같은 계정이나 같은 이메일 계정을 먼저 만들었다.
            return newTransactionTemplate.execute(status -> recoverFromConflict(provider, profile, ex));
        }
    }

    /**
     * 유일 제약 위반 뒤 새 트랜잭션에서 다시 조회한다.
     * 같은 (제공자, 고유 ID) 계정이 있으면 그 계정으로 로그인하고, 같은 이메일 계정이 있으면 409,
     * 둘 다 없으면 원래 예외를 그대로 던진다.
     */
    private LoginResponse recoverFromConflict(
            OAuthProvider provider,
            SocialProfile profile,
            DataIntegrityViolationException ex
    ) {
        Optional<User> sameAccount = userRepository.findByProviderAndProviderUserId(provider, profile.providerUserId());
        if (sameAccount.isPresent()) {
            User user = sameAccount.get();
            user.updateLastLoginAt(LocalDateTime.now());
            return toLoginResponse(user, false);
        }
        if (!isBlank(profile.email())) {
            Optional<User> sameEmail = userRepository.findByEmail(profile.email());
            if (sameEmail.isPresent()) {
                throw new EmailAlreadyRegisteredException(sameEmail.get().getProvider());
            }
        }
        throw ex;
    }

    private LoginResponse loginOrSignup(OAuthProvider provider, SocialProfile profile) {
        LocalDateTime now = LocalDateTime.now();

        // 1. (제공자, 고유 ID)로 찾으면 로그인. 이름·이메일은 가입 때 값 그대로 둔다.
        Optional<User> existing = userRepository.findByProviderAndProviderUserId(provider, profile.providerUserId());
        if (existing.isPresent()) {
            User user = existing.get();
            user.updateLastLoginAt(now);
            return toLoginResponse(user, false);
        }

        // 2. 이메일이 없거나 인증되지 않았으면 가입을 막는다.
        if (isBlank(profile.email()) || !profile.emailVerified()) {
            throw new EmailNotVerifiedException();
        }

        // 3. 같은 이메일로 가입한 계정이 있으면 가입을 막는다.
        userRepository.findByEmail(profile.email()).ifPresent(user -> {
            throw new EmailAlreadyRegisteredException(user.getProvider());
        });

        // 4. 새 계정(LEARNER, ACTIVE)
        User user = new User(provider, profile.providerUserId(), nameOf(profile), profile.email(), now);
        user.updateLastLoginAt(now);
        return toLoginResponse(userRepository.save(user), true);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    private LoginResponse toLoginResponse(User user, boolean newUser) {
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        return new LoginResponse(accessToken, UserResponse.from(user), newUser);
    }

    /**
     * 제공자가 이름을 주지 않으면 이메일의 @ 앞부분을 쓴다(plan.md "계획에서 정한 기본값").
     * users.name 길이(50)를 넘으면 앞 50자만 쓴다.
     */
    private static String nameOf(SocialProfile profile) {
        String name = isBlank(profile.name())
                ? profile.email().substring(0, Math.max(profile.email().indexOf('@'), 0))
                : profile.name().strip();
        if (name.isEmpty()) {
            name = profile.email();
        }
        if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
            name = name.substring(0, name.offsetByCodePoints(0, MAX_NAME_LENGTH));
        }
        return name;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
