package com.maylee.privatelog.service;

import com.maylee.privatelog.dto.user.TokenResponse;
import com.maylee.privatelog.dto.user.UserLoginRequest;
import com.maylee.privatelog.dto.user.UserRegisterRequest;
import com.maylee.privatelog.dto.user.UserResponse;
import com.maylee.privatelog.entity.Users;
import com.maylee.privatelog.repository.UsersRepository;
import com.maylee.privatelog.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public TokenResponse register(UserRegisterRequest request) {
        if (usersRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (usersRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        Users user = Users.register(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );
        usersRepository.save(user);
        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new TokenResponse(token, UserResponse.from(user));
    }

    public TokenResponse login(UserLoginRequest request) {
        Users user = usersRepository.findByUsername(request.username())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }
        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new TokenResponse(token, UserResponse.from(user));
    }

    public UserResponse getUser(Long id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        usersRepository.delete(user);
    }
}
