package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.Customer;
import com.alura.churnnsight.model.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerStatusRepository extends JpaRepository<CustomerStatus,Long> {
    Optional<CustomerStatus> findByCustomer(Customer customer);
}
