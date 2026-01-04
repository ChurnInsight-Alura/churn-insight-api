package com.alura.churnnsight.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")


@Entity(name = "CustomerStatus")
@Table(name = "customer_status")
public class CustomerStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Column(name = "is_active_member", nullable = false)
    private Boolean isActiveMember;
}
