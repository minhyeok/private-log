package com.maylee.privatelog.dto.user;

import com.maylee.privatelog.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank String password,
        @Size(max = 50) String nickname,
        UserRole role
) {
    public UserRole roleOrDefault() {
        return role == null ? UserRole.USER : role;
    }
}
