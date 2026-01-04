package com.alura.churnnsight.dto.consult;

import com.alura.churnnsight.model.Customer;

import java.time.LocalDate;

public record DataCustomerDetail(
        String customerId,
        String geography,
        Integer gender,
        LocalDate birthDate
) {
    public DataCustomerDetail(Customer customer){
        this(
          customer.getCustomerId(),
          customer.getGeography(),
          customer.getGender().getCode(),
          customer.getBirthDate()
        );
    }
}
