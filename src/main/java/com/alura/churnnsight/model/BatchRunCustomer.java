package com.alura.churnnsight.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "batch_run_customer")
@IdClass(BatchRunCustomer.PK.class)
public class BatchRunCustomer {

    @Id
    @Column(name="batch_run_id")
    private Long batchRunId;

    @Id
    @Column(name="customer_id")
    private String customerId;

    public BatchRunCustomer() {}
    public BatchRunCustomer(Long batchRunId, String customerId) {
        this.batchRunId = batchRunId;
        this.customerId = customerId;
    }

    public Long getBatchRunId() { return batchRunId; }
    public String getCustomerId() { return customerId; }

    public static class PK implements Serializable {
        public Long batchRunId;
        public String customerId;

        public PK() {}
        public PK(Long batchRunId, String customerId) {
            this.batchRunId = batchRunId;
            this.customerId = customerId;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(batchRunId, pk.batchRunId) && Objects.equals(customerId, pk.customerId);
        }
        @Override public int hashCode() {
            return Objects.hash(batchRunId, customerId);
        }
    }
}
