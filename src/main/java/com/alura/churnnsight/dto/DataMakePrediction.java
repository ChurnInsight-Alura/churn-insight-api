package com.alura.churnnsight.dto;

import com.alura.churnnsight.model.Customer;
import com.alura.churnnsight.model.enumeration.Plan;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record DataMakePrediction(
        String CustomerId,
        String Geography,
        Integer Gender,
        Integer Age,
        Integer Ternure,
        Double Balance,
        Integer NumOfProducts,
        Integer IsActiveMember
) {

        public DataMakePrediction(Customer customer,Double balance, Integer numOfProducts, Integer isActiveMember){
                this(
                        customer.getCustomerId(),
                        customer.getGeography(),
                        customer.getGender().getCode(),
                        customer.getAge(),
                        customer.getTernure(),
                        balance,
                        numOfProducts,
                        isActiveMember
                );
        }
}


