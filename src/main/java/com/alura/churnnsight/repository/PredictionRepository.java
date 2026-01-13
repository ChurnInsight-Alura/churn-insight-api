package com.alura.churnnsight.repository;

import com.alura.churnnsight.dto.consult.DataPredictionDetail;
import com.alura.churnnsight.model.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction,Long> {
    Page<Prediction> findByCustomerId(Long customerId, Pageable pageable);
    Optional<Prediction> findByCustomerIdAndPredictionDate(Long customerId, LocalDate predictionDate);
    @Query("""
    select p from Prediction p
    join p.customer c
    where p.predictionDate = :bucketDate
    and lower(c.customerId) in :customerIdsLower
    """)
    List<Prediction> findByBucketDateAndCustomerIds(
                @Param("bucketDate") LocalDate bucketDate,
                @Param("customerIdsLower") List<String> customerIdsLower
    );

    @Query("""
        select p
        from Prediction p
        join fetch p.customer c
        where c.id = :customerDbId
          and p.predictionDate = :bucketDate
    """)
    Optional<Prediction> findByCustomerIdAndPredictionDateFetchCustomer(
            @Param("customerDbId") Long customerDbId,
            @Param("bucketDate") LocalDate bucketDate
    );

    @Query("""
          select p
          from Prediction p
          join fetch p.customer c
          where p.predictionDate = :bucketDate
            and lower(c.customerId) in :customerIdsLower
    """)
    List<Prediction> findByBucketDateAndCustomerIdsFetchCustomer(
                @Param("bucketDate") LocalDate bucketDate,
                @Param("customerIdsLower") List<String> customerIdsLower
    );


}
