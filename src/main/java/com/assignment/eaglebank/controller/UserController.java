package com.assignment.eaglebank.controller;

import com.assignment.eaglebank.api.UserApi;
import com.assignment.eaglebank.model.CreateUserRequest;
import com.assignment.eaglebank.model.UpdateUserRequest;
import com.assignment.eaglebank.model.UserResponse;
import com.assignment.eaglebank.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user management operations
 */
@RestController
public class UserController implements UserApi {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
        logger.info("Creating new user with email: {}", createUserRequest.getEmail());
        
        UserResponse response = userService.createUser(createUserRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<UserResponse> fetchUserByID(String userId) {
        logger.debug("Fetching user with ID: {}", userId);
        
        // Get authenticated user ID from security context
        String authenticatedUserId = getAuthenticatedUserId();
        
        UserResponse response = userService.getUserById(userId, authenticatedUserId);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserResponse> updateUserByID(String userId, UpdateUserRequest updateUserRequest) {
        logger.info("Updating user with ID: {}", userId);
        
        // Get authenticated user ID from security context
        String authenticatedUserId = getAuthenticatedUserId();
        
        UserResponse response = userService.updateUser(userId, updateUserRequest, authenticatedUserId);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteUserByID(String userId) {
        logger.info("Deleting user with ID: {}", userId);
        
        // Get authenticated user ID from security context
        String authenticatedUserId = getAuthenticatedUserId();
        
        userService.deleteUser(userId, authenticatedUserId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Get the authenticated user ID from the security context
     */
    private String getAuthenticatedUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        
        return (String) authentication.getPrincipal();
    }
} 