package com.aijapanese.speaking.common.dto;

import com.aijapanese.speaking.user.entity.OAuthProvider;

/**
 * OpenAPI EmailAlreadyRegisteredError: ErrorResponse + registeredProvider(google, kakao).
 */
public record EmailAlreadyRegisteredErrorResponse(
        String code,
        String message,
        OAuthProvider registeredProvider
) {
}
