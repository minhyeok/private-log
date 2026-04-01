package com.maylee.privatelog.dto.user;

import com.maylee.privatelog.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 50) String username,
        @Email @Size(max = 100) String email,
        String password,
        @Size(max = 50) String nickname,
        UserRole role
) {
}
