package com.assignment.eaglebank.repository;

import com.assignment.eaglebank.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for UserEntity operations
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * Find user by ID and not deleted
     */
    Optional<UserEntity> findByIdAndDeletedFalse(String id);

    /**
     * Find user by email and not deleted
     */
    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    /**
     * Check if user exists by email and not deleted
     */
    boolean existsByEmailAndDeletedFalse(String email);

    /**
     * Check if user has any accounts
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AccountEntity a WHERE a.user.id = :userId")
    boolean hasAccounts(@Param("userId") String userId);
} 