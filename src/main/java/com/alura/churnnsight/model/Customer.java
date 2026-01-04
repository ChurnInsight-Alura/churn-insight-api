package com.alura.churnnsight.model;

import com.alura.churnnsight.dto.creation.DataCreateCustomer;
import com.alura.churnnsight.model.enumeration.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")

@Entity( name = "Customer")
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;

    @Column(nullable = false)
    private String geography;

    @Column(name = "gender", nullable = false)
    private Gender gender; // 0 = Female, 1 = Male

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDate createdAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Account> accounts = new HashSet<>();

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private CustomerStatus status;

    @ManyToMany
    @JoinTable(
            name = "customer_product",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products = new HashSet<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Prediction> predictions = new ArrayList<>();

    public Customer(DataCreateCustomer data){
        this.customerId = data.customerId();
        this.geography = data.geography();
        this.gender = Gender.fromCode(data.gender());
        this.birthDate = data.birthDate();
        this.createdAt = data.createdAt();

    }

    public int getAge(){
            return (int) birthDate.until(LocalDate.now(),ChronoUnit.YEARS);
    }
    public int getTernure(){
            return (int) createdAt.until(LocalDate.now(),ChronoUnit.MONTHS);
    }

    public void addProduct(Product product) {
        this.products.add(product);
    }
}
