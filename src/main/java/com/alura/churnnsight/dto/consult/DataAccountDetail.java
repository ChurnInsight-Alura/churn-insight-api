package com.alura.churnnsight.dto.consult;

import com.alura.churnnsight.model.Account;

public record DataAccountDetail(
        String customerId,
        Double balance
) {
    public DataAccountDetail(Account account){
        this(
          account.getCustomer().getCustomerId(),
          account.getBalance()
        );
    }
}
