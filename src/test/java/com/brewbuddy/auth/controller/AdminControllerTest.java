package com.brewbuddy.auth.controller;

import com.brewbuddy.auth.dto.ChangePasswordRequest;
import com.brewbuddy.auth.dto.CreateUserRequest;
import com.brewbuddy.auth.dto.UpdateUserRequest;
import com.brewbuddy.auth.dto.UserResponse;
import com.brewbuddy.auth.filter.JwtAuthenticationFilter;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {JwtAuthenticationFilter.class}))
@Import(com.brewbuddy.auth.config.TestSecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private UserResponse userResponse;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = new UserResponse(
            userId,
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
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldAccessAdminDashboard() throws Exception {
        // given
        UserDetails admin = User.builder()
            .username("admin")
            .password("password")
            .authorities("ROLE_ADMIN")
            .build();

        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .with(user(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Welcome to admin dashboard"))
            .andExpect(jsonPath("$.admin").value("admin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldCreateUser() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest(
            "newuser",
            "new@example.com",
            "password123",
            "New",
            "User",
            false,
            true,
            Set.of("USER")
        );
        given(userService.createUser(any(CreateUserRequest.class)))
            .willReturn(userResponse);

        // when & then
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).createUser(any(CreateUserRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetAllUsers() throws Exception {
        // given
        given(userService.getAllUsers())
            .willReturn(List.of(userResponse));

        // when & then
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].username").value("testuser"))
            .andExpect(jsonPath("$[0].email").value("test@example.com"));

        verify(userService).getAllUsers();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetUserById() throws Exception {
        // given
        given(userService.getUserById(userId))
            .willReturn(userResponse);

        // when & then
        mockMvc.perform(get("/api/admin/users/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).getUserById(userId);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUpdateUser() throws Exception {
        // given
        UpdateUserRequest request = new UpdateUserRequest(
            "updateduser",
            "updated@example.com",
            "Updated",
            "User",
            true,
            true,
            Set.of("USER", "ADMIN")
        );

        UserResponse updatedResponse = new UserResponse(
            userId,
            "updateduser",
            "updated@example.com",
            "Updated",
            "User",
            true,
            true,
            Set.of("USER", "ADMIN"),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        given(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
            .willReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/api/admin/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("updateduser"))
            .andExpect(jsonPath("$.email").value("updated@example.com"));

        verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldDeleteUser() throws Exception {
        // given
        willDoNothing().given(userService).deleteUser(userId);

        // when & then
        mockMvc.perform(delete("/api/admin/users/" + userId))
            .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldChangePassword() throws Exception {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest(
            "newpassword123",
            false
        );
        willDoNothing().given(userService).changePassword(eq(userId), any(ChangePasswordRequest.class));

        // when & then
        mockMvc.perform(post("/api/admin/users/" + userId + "/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(userService).changePassword(eq(userId), any(ChangePasswordRequest.class));
    }
}
