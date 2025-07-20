package com.assignment.eaglebank.repository;

import com.assignment.eaglebank.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AccountEntity operations
 */
@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    /**
     * Find account by formatted account number (01XXXXXX format)
     */
    @Query("SELECT a FROM AccountEntity a WHERE CONCAT('01', LPAD(CAST(a.accountNumber AS string), 6, '0')) = :formattedAccountNumber")
    Optional<AccountEntity> findByFormattedAccountNumber(@Param("formattedAccountNumber") String formattedAccountNumber);

    /**
     * Find all accounts for a specific user
     */
    @Query("SELECT a FROM AccountEntity a WHERE a.user.id = :userId ORDER BY a.createdTimestamp DESC")
    List<AccountEntity> findByUserId(@Param("userId") String userId);
} 