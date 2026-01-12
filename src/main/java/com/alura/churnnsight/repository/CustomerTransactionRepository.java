package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerTransactionRepository
        extends JpaRepository<CustomerTransaction, Long> {

    List<CustomerTransaction> findByCustomerId(Long customerId);
    Optional<CustomerTransaction> findByTransactionId(String transactionId);

}

