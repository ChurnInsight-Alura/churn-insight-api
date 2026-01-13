package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.Account;
import com.alura.churnnsight.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {
    Optional<Account> findByCustomer(Customer customer);
}
