package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.Account;
import com.alura.churnnsight.model.Customer;
import com.alura.churnnsight.model.CustomerStatus;
import com.alura.churnnsight.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer,Long> {
    @Query("SELECT COUNT(p) FROM Customer c JOIN c.products p WHERE c.id = :customerId ")
    Integer CountProductsByCostumerId(@Param("customerId") Long id);

    @Query("SELECT SUM(a.balance) FROM Customer c JOIN c.accounts a WHERE c.id = :customerId")
    Float CountBalanceByCostumerId(@Param("customerId") Long id);

    Optional<Customer> findByCustomerIdIgnoreCase(String customerId);

    @Query("SELECT p FROM Customer c JOIN c.products p WHERE c.id = :customerId")
    Page<Product> findProductsByCustomerId( @Param("customerId") Long id, Pageable pageable);

    @Query("SELECT c.status FROM Customer c WHERE c.id = :customerId")
    CustomerStatus findStatusByCustomerId(@Param("customerId") Long id);

    @Query("SELECT a FROM Customer c JOIN c.accounts a WHERE c.id = :customerId")
    Page<Account> findAccountsByCustomerId(@Param("customerId") Long id, Pageable pageable);

    Optional<Customer> findByCustomerId(String customerId);
}
