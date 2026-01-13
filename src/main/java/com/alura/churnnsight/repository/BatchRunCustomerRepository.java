package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.BatchRunCustomer;
import com.alura.churnnsight.model.BatchRunCustomer.PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BatchRunCustomerRepository extends JpaRepository<BatchRunCustomer, PK> {

    @Query("select brc.customerId from BatchRunCustomer brc where brc.batchRunId = :batchRunId")
    List<String> findCustomerIdsByBatchRunId(Long batchRunId);
}
