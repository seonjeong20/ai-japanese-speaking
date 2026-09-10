package com.aijapanese.speaking.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String password,
        @NotNull SignupRole role,
        @NotNull Long organizationId,
        @Size(max = 100) String department
) {
}
