package com.alura.churnnsight.dto.consult;

import com.alura.churnnsight.model.CustomerStatus;

public record DataCustomerStatusDetail(
        String customerId,
        Integer creditScore,
        Boolean isActiveMember
) {
    public DataCustomerStatusDetail(CustomerStatus customerStatus){
        this(
          customerStatus.getCustomer().getCustomerId(),
          customerStatus.getCreditScore(),
          customerStatus.getIsActiveMember()
        );
    }
}
