package com.aijapanese.speaking.common.exception;

import com.aijapanese.speaking.auth.exception.AccountNotActiveException;
import com.aijapanese.speaking.auth.exception.EmailAlreadyExistsException;
import com.aijapanese.speaking.auth.exception.InvalidCredentialsException;
import com.aijapanese.speaking.auth.exception.OrganizationNotAvailableException;
import com.aijapanese.speaking.auth.exception.UnsupportedSignupRoleException;
import com.aijapanese.speaking.ai.AiServiceException;
import com.aijapanese.speaking.common.dto.ErrorResponse;
import com.aijapanese.speaking.conversation.exception.ConversationFeedbackNotFoundException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionAccessDeniedException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotFoundException;
import com.aijapanese.speaking.speaking.exception.SpeakingSessionNotInProgressException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("EMAIL_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(OrganizationNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationNotAvailable(OrganizationNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ORGANIZATION_NOT_AVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedSignupRoleException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedSignupRole(UnsupportedSignupRoleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("UNSUPPORTED_SIGNUP_ROLE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActive(AccountNotActiveException ex) {
        String code = switch (ex.getStatus()) {
            case PENDING -> "ACCOUNT_PENDING";
            case REJECTED -> "ACCOUNT_REJECTED";
            case INACTIVE -> "ACCOUNT_INACTIVE";
            case ACTIVE -> "ACCOUNT_NOT_ACTIVE";
        };
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(code, ex.getMessage()));
    }

    @ExceptionHandler(SpeakingSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSpeakingSessionNotFound(SpeakingSessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SESSION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(SpeakingSessionAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpeakingSessionAccessDenied(SpeakingSessionAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("SESSION_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(SpeakingSessionNotInProgressException.class)
    public ResponseEntity<ErrorResponse> handleSpeakingSessionNotInProgress(SpeakingSessionNotInProgressException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SESSION_NOT_IN_PROGRESS", ex.getMessage()));
    }

    @ExceptionHandler(ConversationFeedbackNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationFeedbackNotFound(ConversationFeedbackNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("FEEDBACK_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ErrorResponse> handleAiServiceException(AiServiceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("AI_SERVICE_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", "요청 값이 올바르지 않습니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", "요청 값이 올바르지 않습니다."));
    }
}
