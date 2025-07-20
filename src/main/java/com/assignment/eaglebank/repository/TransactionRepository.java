package com.assignment.eaglebank.repository;

import com.assignment.eaglebank.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TransactionEntity operations
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    /**
     * Find all transactions for a specific account ordered by creation timestamp desc
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.account.accountNumber = :accountNumber ORDER BY t.createdTimestamp DESC")
    List<TransactionEntity> findByAccountNumberOrderByCreatedTimestampDesc(@Param("accountNumber") Long accountNumber);

    /**
     * Find transaction by ID and verify it belongs to a specific user (for security)
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.id = :transactionId AND t.account.user.id = :userId")
    Optional<TransactionEntity> findByIdAndUserId(@Param("transactionId") String transactionId, @Param("userId") String userId);
} 