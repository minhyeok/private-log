package com.maylee.privatelog.security;

import com.maylee.privatelog.entity.UserRole;

public record AuthUser(Long id, String username, UserRole role) {}
