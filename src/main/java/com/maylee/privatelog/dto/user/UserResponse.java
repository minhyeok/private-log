package com.maylee.privatelog.dto.user;

import com.maylee.privatelog.entity.Users;

public record UserResponse(
        Long id,
        String username,
        String nickname
) {
    public static UserResponse from(Users user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getNickname());
    }
}
