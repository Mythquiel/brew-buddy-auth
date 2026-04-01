package com.brewbuddy.auth.controller;

import com.brewbuddy.auth.dto.UpdateUserRequest;
import com.brewbuddy.auth.dto.UserResponse;
import com.brewbuddy.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserProfileController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieve the authenticated user's profile information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token"
        )
    })
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    @Operation(
        summary = "Update current user profile",
        description = "Update the authenticated user's profile. Users can only update their own username, email, firstName, and lastName. Roles and status cannot be self-modified."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token"
        )
    })
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserRequest request) {

        // Get current user
        UserResponse currentUser = userService.getUserByUsername(userDetails.getUsername());

        // Users cannot change their own roles or enabled status
        UpdateUserRequest safeRequest = new UpdateUserRequest(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                null, // email verified cannot be changed by user
                null, // enabled cannot be changed by user
                null  // roles cannot be changed by user
        );

        UserResponse updatedUser = userService.updateUser(currentUser.id(), safeRequest);
        return ResponseEntity.ok(updatedUser);
    }
}
