package com.alura.churnnsight.dto;

import com.alura.churnnsight.model.Customer;

public record DataMakePrediction(
        String CustomerId,
        String Geography,
        Integer Gender,
        Integer Age,
        Integer Tenure,
        Float Balance,
        Integer NumOfProducts,
        Integer IsActiveMember
) {

        public DataMakePrediction(Customer customer,Integer tenure,Float balance, Integer numOfProducts, Integer isActiveMember){
                this(
                        customer.getCustomerId(),
                        customer.getGeography(),
                        customer.getGender().getCode(),
                        customer.getAge(),
                        tenure,
                        balance,
                        numOfProducts,
                        isActiveMember
                );
        }
}


