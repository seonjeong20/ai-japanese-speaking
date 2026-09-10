package com.aijapanese.speaking.auth.exception;

import com.aijapanese.speaking.user.entity.UserStatus;

public class AccountNotActiveException extends RuntimeException {

    private final UserStatus status;

    public AccountNotActiveException(UserStatus status, String message) {
        super(message);
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }
}
