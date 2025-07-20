package com.assignment.eaglebank.service;

import com.assignment.eaglebank.entity.UserEntity;
import com.assignment.eaglebank.exception.BusinessRuleViolationException;
import com.assignment.eaglebank.exception.ResourceNotFoundException;
import com.assignment.eaglebank.exception.AccessDeniedException;
import com.assignment.eaglebank.model.AuthenticationResponse;
import com.assignment.eaglebank.model.CreateUserRequest;
import com.assignment.eaglebank.model.UpdateUserRequest;
import com.assignment.eaglebank.model.UserResponse;
import com.assignment.eaglebank.model.CreateUserRequestAddress;
import com.assignment.eaglebank.repository.UserRepository;
import com.assignment.eaglebank.security.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/**
 * Service class for user management operations
 */
@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenManager tokenManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenManager = tokenManager;
    }

    /**
     * Create a new user
     */
    public UserResponse createUser(CreateUserRequest request) {
        logger.info("Creating new user with email: {}", request.getEmail());

        // Validate phone number format
        validatePhoneNumber(request.getPhoneNumber());

        // Check if user already exists
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new BusinessRuleViolationException("User with email " + request.getEmail() + " already exists");
        }

        // Create user entity
        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        
        // Set address fields
        CreateUserRequestAddress address = request.getAddress();
        user.setAddressLine1(address.getLine1());
        user.setAddressLine2(address.getLine2());
        user.setAddressLine3(address.getLine3());
        user.setTown(address.getTown());
        user.setCounty(address.getCounty());
        user.setPostcode(address.getPostcode());

        // Encode password from request
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Save user
        user = userRepository.save(user);

        logger.info("Created user with ID: {}", user.getId());
        return toUserResponse(user);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId, String authenticatedUserId) {
        logger.debug("Fetching user with ID: {}", userId);

        // Verify user access
        if (!userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only access your own user information");
        }

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return toUserResponse(user);
    }

    /**
     * Update user information
     */
    public UserResponse updateUser(String userId, UpdateUserRequest request, String authenticatedUserId) {
        logger.info("Updating user with ID: {}", userId);

        // Verify user access
        if (!userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only update your own user information");
        }

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update fields if provided
        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null) {
            // Check if email is already taken by another user
            if (!request.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
                throw new BusinessRuleViolationException("Email " + request.getEmail() + " is already taken");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            validatePhoneNumber(request.getPhoneNumber());
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // Update address if provided
        if (request.getAddress() != null) {
            CreateUserRequestAddress address = request.getAddress();
            if (address.getLine1() != null) user.setAddressLine1(address.getLine1());
            if (address.getLine2() != null) user.setAddressLine2(address.getLine2());
            if (address.getLine3() != null) user.setAddressLine3(address.getLine3());
            if (address.getTown() != null) user.setTown(address.getTown());
            if (address.getCounty() != null) user.setCounty(address.getCounty());
            if (address.getPostcode() != null) user.setPostcode(address.getPostcode());
        }

        user = userRepository.save(user);

        logger.info("Updated user with ID: {}", userId);
        return toUserResponse(user);
    }

    /**
     * Delete user (soft delete)
     */
    public void deleteUser(String userId, String authenticatedUserId) {
        logger.info("Deleting user with ID: {}", userId);

        // Verify user access
        if (!userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("You can only delete your own account");
        }

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Check if user has accounts
        if (userRepository.hasAccounts(userId)) {
            throw new BusinessRuleViolationException("Cannot delete user with active accounts");
        }

        // Soft delete
        user.setDeleted(true);
        userRepository.save(user);

        logger.info("Deleted user with ID: {}", userId);
    }

    /**
     * Authenticate user and generate JWT token
     * Returns the generated AuthenticationResponse from OpenAPI contract
     */
    public AuthenticationResponse validateCredentials(String email, String password) {
        logger.info("Authenticating user with email: {}", email);

        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = tokenManager.createAuthToken(user.getId(), user.getEmail());
        logger.info("Generated JWT token for user: {}", user.getId());
        
        // Return generated AuthenticationResponse from OpenAPI contract
        return new AuthenticationResponse()
            .token(token)
            .userId(user.getId());
    }

    /**
     * Convert UserEntity to UserResponse
     */
    private UserResponse toUserResponse(UserEntity user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setCreatedTimestamp(user.getCreatedTimestamp());
        response.setUpdatedTimestamp(user.getUpdatedTimestamp());

        // Set address
        CreateUserRequestAddress address = new CreateUserRequestAddress();
        address.setLine1(user.getAddressLine1());
        address.setLine2(user.getAddressLine2());
        address.setLine3(user.getAddressLine3());
        address.setTown(user.getTown());
        address.setCounty(user.getCounty());
        address.setPostcode(user.getPostcode());
        response.setAddress(address);

        return response;
    }

    /**
     * Validate phone number format according to OpenAPI specification
     */
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        // Pattern matches the OpenAPI specification: ^\+[1-9]\d{1,14}$
        Pattern phonePattern = Pattern.compile("^\\+[1-9]\\d{1,14}$");
        
        if (!phonePattern.matcher(phoneNumber.trim()).matches()) {
            throw new IllegalArgumentException("Invalid phone number format. Phone number must start with + followed by 1-9 and then 1-14 digits (e.g., +1234567890)");
        }
    }
} 