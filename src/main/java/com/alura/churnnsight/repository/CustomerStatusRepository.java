package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerStatusRepository extends JpaRepository<CustomerStatus,Long> {
}
