package com.brewbuddy.auth.controller;

import com.brewbuddy.auth.dto.AuthenticationResponse;
import com.brewbuddy.auth.dto.LoginRequest;
import com.brewbuddy.auth.dto.RefreshTokenRequest;
import com.brewbuddy.auth.dto.RegisterRequest;
import com.brewbuddy.auth.dto.UserResponse;
import com.brewbuddy.auth.filter.JwtAuthenticationFilter;
import com.brewbuddy.auth.service.AuthenticationService;
import com.brewbuddy.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {JwtAuthenticationFilter.class}))
@Import(com.brewbuddy.auth.config.TestSecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserService userService;

    private UserResponse userResponse;
    private AuthenticationResponse authResponse;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponse(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            "Test",
            "User",
            false,
            true,
            Set.of("USER"),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        authResponse = new AuthenticationResponse(
            "access-token",
            "refresh-token",
            "Bearer",
            86400L,
            userResponse
        );
    }

    @Test
    void shouldRegisterNewUser() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "new@example.com",
            "password123",
            "New",
            "User"
        );
        given(authenticationService.register(any(RegisterRequest.class)))
            .willReturn(authResponse);

        // when & then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    void shouldRejectInvalidRegistration() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest(
            "a", // Too short
            "invalid-email",
            "short",
            null,
            null
        );

        // when & then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // given
        LoginRequest request = new LoginRequest("testuser", "password123");
        given(authenticationService.login(any(LoginRequest.class)))
            .willReturn(authResponse);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    void shouldRejectLoginWithEmptyCredentials() throws Exception {
        // given
        LoginRequest request = new LoginRequest("", "");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRefreshToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        given(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
            .willReturn(authResponse);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldValidateToken() throws Exception {
        // given
        given(userService.getUserByUsername("testuser"))
            .willReturn(userResponse);

        UserDetails userDetails = User.builder()
            .username("testuser")
            .password("password")
            .authorities("ROLE_USER")
            .build();

        // when & then
        mockMvc.perform(get("/api/auth/validate")
                .with(user(userDetails)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldLogout() throws Exception {
        // given - no setup needed

        // when & then
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out successfully. Please discard your token."));
    }
}
