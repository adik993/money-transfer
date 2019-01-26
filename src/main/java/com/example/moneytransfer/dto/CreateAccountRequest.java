package com.example.moneytransfer.dto;

import lombok.Value;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

@Value
public class CreateAccountRequest {
    private BigDecimal balance;

    public void validate() {
        if (ZERO.compareTo(balance) > 0) throw new IllegalStateException("balance must me zero or greater");
    }
}
