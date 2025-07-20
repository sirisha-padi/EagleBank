package com.assignment.eaglebank.controller;

import com.assignment.eaglebank.api.AuthApi;
import com.assignment.eaglebank.model.AuthenticationRequest;
import com.assignment.eaglebank.model.AuthenticationResponse;
import com.assignment.eaglebank.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations
 * Implements the generated AuthApi interface from OpenAPI specification
 */
@RestController
public class AuthenticationController implements AuthApi {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    private final UserService userService;

    public AuthenticationController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Authenticate user credentials and return JWT token
     * 
     * @param authenticationRequest User credentials (email and password)
     * @return ResponseEntity containing authentication token and user ID
     */
    @Override
    public ResponseEntity<AuthenticationResponse> authenticateUser(AuthenticationRequest authenticationRequest) {
        logger.info("Processing authentication request for email: {}", authenticationRequest.getEmail());
        
        // Validate credentials and get authentication response
        AuthenticationResponse response = userService.validateCredentials(
            authenticationRequest.getEmail(), 
            authenticationRequest.getPassword()
        );
        
        logger.info("Authentication successful for user: {}", response.getUserId());
        
        return ResponseEntity.ok(response);
    }
} 