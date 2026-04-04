package com.brewbuddy.auth.service;

import com.brewbuddy.auth.dto.ChangePasswordRequest;
import com.brewbuddy.auth.dto.CreateUserRequest;
import com.brewbuddy.auth.dto.UpdateUserRequest;
import com.brewbuddy.auth.dto.UserResponse;
import com.brewbuddy.auth.entity.User;
import com.brewbuddy.auth.entity.UserRole;
import com.brewbuddy.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username()) ||
            userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User creation failed. Username or email may already be in use.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmailVerified(request.emailVerified());
        user.setEnabled(request.enabled());

        for (String roleName : request.roles()) {
            UserRole role = new UserRole();
            role.setUser(user);
            role.setRoleName(roleName);
            user.getRoles().add(role);
        }

        user = userRepository.save(user);

        return mapToResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return mapToResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (request.username() != null) {
            if (!request.username().equals(user.getUsername()) &&
                userRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Username already exists");
            }
            user.setUsername(request.username());
        }

        if (request.email() != null) {
            if (!request.email().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(request.email());
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        if (request.emailVerified() != null) {
            user.setEmailVerified(request.emailVerified());
        }

        if (request.roles() != null) {
            user.getRoles().clear();

            for (String roleName : request.roles()) {
                UserRole role = new UserRole();
                role.setUser(user);
                role.setRoleName(roleName);
                user.getRoles().add(role);
            }
        }

        user = userRepository.save(user);

        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(UserRole::getRoleName)
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmailVerified(),
                user.getEnabled(),
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
