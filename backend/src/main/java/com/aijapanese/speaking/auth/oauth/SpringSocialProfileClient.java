package com.aijapanese.speaking.auth.oauth;

import com.aijapanese.speaking.auth.exception.InvalidAuthorizationCodeException;
import com.aijapanese.speaking.auth.exception.OAuthProviderException;
import com.aijapanese.speaking.user.entity.OAuthProvider;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Set;

/**
 * Spring Security OAuth2 Client 부품으로 인가 코드를 토큰으로 바꾸고 사용자 정보를 받아 온다.
 * oauth2Login() 흐름은 쓰지 않는다. 제공자 토큰은 이 메서드 밖으로 내보내지 않고 저장하지도 않는다.
 * 인가 코드·토큰·Client Secret은 로그에 남기지 않는다.
 */
@Component
public class SpringSocialProfileClient implements SocialProfileClient {

    /** 제공자가 코드를 거절할 때 쓰는 오류 코드. 이때만 400, 나머지는 502로 본다. */
    private static final Set<String> REJECTED_CODE_ERRORS = Set.of("invalid_grant", "invalid_request");

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestClientAuthorizationCodeTokenResponseClient tokenResponseClient =
            new RestClientAuthorizationCodeTokenResponseClient();
    private final DefaultOAuth2UserService userService = new DefaultOAuth2UserService();

    public SpringSocialProfileClient(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public SocialProfile fetch(OAuthProvider provider, String code, String redirectUri) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider.apiValue());
        if (registration == null) {
            throw new IllegalStateException("OAuth 등록이 없습니다: " + provider.apiValue());
        }

        OAuth2AccessTokenResponse tokenResponse = exchange(registration, code, redirectUri);
        OAuth2User oauth2User = loadUser(registration, tokenResponse);
        return toProfile(provider, oauth2User.getAttributes());
    }

    private OAuth2AccessTokenResponse exchange(ClientRegistration registration, String code, String redirectUri) {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(redirectUri)
                .scopes(registration.getScopes())
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(code)
                .redirectUri(redirectUri)
                .build();
        OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(
                registration,
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse)
        );

        try {
            return tokenResponseClient.getTokenResponse(grantRequest);
        } catch (OAuth2AuthorizationException ex) {
            if (REJECTED_CODE_ERRORS.contains(ex.getError().getErrorCode())) {
                throw new InvalidAuthorizationCodeException();
            }
            throw new OAuthProviderException(ex);
        } catch (RestClientException ex) {
            throw new OAuthProviderException(ex);
        }
    }

    private OAuth2User loadUser(ClientRegistration registration, OAuth2AccessTokenResponse tokenResponse) {
        try {
            return userService.loadUser(new OAuth2UserRequest(registration, tokenResponse.getAccessToken()));
        } catch (OAuth2AuthenticationException | RestClientException ex) {
            throw new OAuthProviderException(ex);
        }
    }

    /**
     * 사용자 정보 응답을 SocialProfile로 바꾼다. 빠진 값은 null·false로 둔다.
     * 구글: sub, email, email_verified, name
     * 카카오: id, kakao_account.email, kakao_account.is_email_verified, kakao_account.profile.nickname
     */
    public static SocialProfile toProfile(OAuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new SocialProfile(
                    provider,
                    stringValue(attributes.get("sub")),
                    stringValue(attributes.get("email")),
                    isTrue(attributes.get("email_verified")),
                    stringValue(attributes.get("name"))
            );
            case KAKAO -> {
                Map<?, ?> account = mapValue(attributes.get("kakao_account"));
                Map<?, ?> profile = mapValue(account.get("profile"));
                yield new SocialProfile(
                        provider,
                        stringValue(attributes.get("id")),
                        stringValue(account.get("email")),
                        isTrue(account.get("is_email_verified")),
                        stringValue(profile.get("nickname"))
                );
            }
        };
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    /** true 또는 "true"만 참으로 본다. 빠졌거나 다른 값이면 거짓이다. */
    private static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    private static Map<?, ?> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }
}
