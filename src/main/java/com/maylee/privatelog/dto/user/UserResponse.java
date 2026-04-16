package com.maylee.privatelog.dto.user;

import com.maylee.privatelog.entity.Users;

public record UserResponse(
        Long id,
        String username,
        String email,
        String nickname,
        String role
) {
    public static UserResponse from(Users user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name()
        );
    }
}
