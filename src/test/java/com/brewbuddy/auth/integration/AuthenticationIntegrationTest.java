package com.brewbuddy.auth.integration;

import com.brewbuddy.auth.dto.LoginRequest;
import com.brewbuddy.auth.dto.RegisterRequest;
import com.brewbuddy.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCompleteFullAuthenticationFlow() throws Exception {
        // 1. Register new user
        RegisterRequest registerRequest = new RegisterRequest(
            "integrationuser",
            "integration@example.com",
            "Password123!",
            "Integration",
            "User"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.username").value("integrationuser"))
            .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(registerResponse).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(registerResponse).get("refreshToken").asText();

        // 2. Validate token
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("integrationuser"));

        // 3. Access user profile
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("integrationuser"))
            .andExpect(jsonPath("$.email").value("integration@example.com"));

        // 4. Logout
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());

        // 5. Login again
        LoginRequest loginRequest = new LoginRequest("integrationuser", "Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.user.username").value("integrationuser"))
            .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

        // 6. Access protected endpoint with new token
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + newAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("integrationuser"));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        // Register first user
        RegisterRequest firstRequest = new RegisterRequest(
            "duplicateuser",
            "first@example.com",
            "Password123!",
            "First",
            "User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isOk());

        // Try to register with same username
        RegisterRequest duplicateRequest = new RegisterRequest(
            "duplicateuser",
            "second@example.com",
            "Password123!",
            "Second",
            "User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest(
            "validuser",
            "valid@example.com",
            "CorrectPassword123!",
            "Valid",
            "User"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        // Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest("validuser", "WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isForbidden());
    }
}
