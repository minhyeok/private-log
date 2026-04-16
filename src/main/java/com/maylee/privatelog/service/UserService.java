package com.maylee.privatelog.service;

import com.maylee.privatelog.dto.user.UserCreateRequest;
import com.maylee.privatelog.dto.user.UserResponse;
import com.maylee.privatelog.dto.user.UserUpdateRequest;
import com.maylee.privatelog.entity.Users;
import com.maylee.privatelog.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UsersRepository usersRepository;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        Users user = Users.builder()
                .username(request.username())
                .email(request.email())
                .password(request.password())
                .nickname(request.nickname())
                .role(request.roleOrDefault())
                .build();

        return UserResponse.from(usersRepository.save(user));
    }

    public UserResponse getUser(Long userId) {
        return UserResponse.from(getById(userId));
    }

    public List<UserResponse> getUsers() {
        return usersRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        Users user = getById(userId);

        user.update(
                request.username(),
                request.email(),
                request.password(),
                request.nickname(),
                request.role()
        );

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        Users user = getById(userId);
        usersRepository.delete(user);
    }

    private Users getById(Long userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    }
}
