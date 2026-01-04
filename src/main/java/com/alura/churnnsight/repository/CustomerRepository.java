package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer,Long> {
    @Query("SELECT COUNT(p) FROM Customer c JOIN c.products p WHERE c.id = :customerId ")
    Integer CountProductsByCostumerId(@Param("customerId") Long id);

    @Query("SELECT SUM(a.balance) FROM Customer c JOIN c.accounts a WHERE c.id = :customerId")
    Float CountBalanceByCostumerId(@Param("customerId") Long id);

    Optional<Customer> findByCustomerIdIgnoreCase(String customerId);
}
