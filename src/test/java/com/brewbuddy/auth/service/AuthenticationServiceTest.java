package com.brewbuddy.auth.service;

import com.brewbuddy.auth.dto.AuthenticationResponse;
import com.brewbuddy.auth.dto.LoginRequest;
import com.brewbuddy.auth.dto.RefreshTokenRequest;
import com.brewbuddy.auth.dto.RegisterRequest;
import com.brewbuddy.auth.entity.User;
import com.brewbuddy.auth.entity.UserRole;
import com.brewbuddy.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "expirationMs", 86400000L);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$12$hashedpassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setEmailVerified(false);

        UserRole role = new UserRole();
        role.setRoleName("USER");
        role.setUser(testUser);
        testUser.getRoles().add(role);
    }

    @Test
    void shouldRegisterNewUser() {
        // given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "new@example.com",
            "password123",
            "New",
            "User"
        );

        given(userRepository.existsByUsername("newuser")).willReturn(false);
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("$2a$12$hashedpassword");
        given(userRepository.save(any(User.class))).willReturn(testUser);
        given(userDetailsService.loadUserByUsername("testuser")).willReturn(mock(UserDetails.class));
        given(jwtService.generateAccessToken(any())).willReturn("access-token");
        given(jwtService.generateRefreshToken(any())).willReturn("refresh-token");

        // when
        AuthenticationResponse response = authenticationService.register(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("testuser");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$12$hashedpassword");
        assertThat(savedUser.getRoles()).hasSize(1);
        assertThat(savedUser.getRoles().iterator().next().getRoleName()).isEqualTo("USER");
    }

    @Test
    void shouldThrowExceptionWhenRegisteringExistingUsername() {
        // given
        RegisterRequest request = new RegisterRequest(
            "existinguser",
            "new@example.com",
            "password123",
            "New",
            "User"
        );
        given(userRepository.existsByUsername("existinguser")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authenticationService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenRegisteringExistingEmail() {
        // given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "existing@example.com",
            "password123",
            "New",
            "User"
        );
        given(userRepository.existsByUsername("newuser")).willReturn(false);
        given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authenticationService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfully() {
        // given
        LoginRequest request = new LoginRequest("testuser", "password123");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
        given(userDetailsService.loadUserByUsername("testuser")).willReturn(mock(UserDetails.class));
        given(jwtService.generateAccessToken(any())).willReturn("access-token");
        given(jwtService.generateRefreshToken(any())).willReturn("refresh-token");

        // when
        AuthenticationResponse response = authenticationService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().username()).isEqualTo("testuser");

        verify(authenticationManager).authenticate(
            any(UsernamePasswordAuthenticationToken.class)
        );
    }

    @Test
    void shouldThrowExceptionForInvalidCredentials() {
        // given
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        willThrow(new BadCredentialsException("Bad credentials"))
            .given(authenticationManager).authenticate(any());

        // when & then
        assertThatThrownBy(() -> authenticationService.login(request))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRefreshToken() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        UserDetails userDetails = mock(UserDetails.class);

        given(jwtService.extractUsername("valid-refresh-token")).willReturn("testuser");
        given(userDetailsService.loadUserByUsername("testuser")).willReturn(userDetails);
        given(jwtService.isTokenValid("valid-refresh-token", userDetails)).willReturn(true);
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
        given(jwtService.generateAccessToken(userDetails)).willReturn("new-access-token");

        // when
        AuthenticationResponse response = authenticationService.refreshToken(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("valid-refresh-token");
        assertThat(response.user().username()).isEqualTo("testuser");

        verify(jwtService).isTokenValid("valid-refresh-token", userDetails);
    }

    @Test
    void shouldThrowExceptionForInvalidRefreshToken() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        UserDetails userDetails = mock(UserDetails.class);
        given(jwtService.extractUsername("invalid-refresh-token")).willReturn("testuser");
        given(userDetailsService.loadUserByUsername("testuser")).willReturn(userDetails);
        given(jwtService.isTokenValid("invalid-refresh-token", userDetails)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authenticationService.refreshToken(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid refresh token");

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringLogin() {
        // given
        LoginRequest request = new LoginRequest("nonexistent", "password");
        given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authenticationService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }
}
