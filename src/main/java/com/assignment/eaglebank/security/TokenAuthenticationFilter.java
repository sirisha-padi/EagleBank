package com.assignment.eaglebank.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT Authentication Filter that processes JWT tokens from HTTP requests.
 * This filter extracts and validates JWT tokens from the Authorization header.
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticationFilter.class);
    
    @Autowired
    private TokenManager tokenManager;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");
        
        String userId = null;
        String jwt = null;
        
        // Extract JWT token from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                userId = tokenManager.getUserIdFromToken(jwt);
                logger.debug("Extracted user ID from JWT: {}", userId);
            } catch (Exception e) {
                logger.warn("Invalid JWT token: {}", e.getMessage());
            }
        }
        
        // If token is valid and user is not already authenticated
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (tokenManager.verifyAuthToken(jwt)) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Successfully authenticated user: {}", userId);
                } else {
                    logger.warn("JWT token validation failed for user: {}", userId);
                }
            } catch (Exception e) {
                logger.error("Error validating JWT token for user {}: {}", userId, e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 