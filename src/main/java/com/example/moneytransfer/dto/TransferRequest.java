package com.example.moneytransfer.dto;

import com.example.moneytransfer.model.Transfer;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.time.Clock.systemUTC;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;

@Value
public class TransferRequest {
    private long fromAccount;
    private long toAccount;
    private BigDecimal amount;

    public List<Long> getAccountIds() {
        return asList(fromAccount, toAccount);
    }

    public void validate() {
        if (ZERO.compareTo(amount) >= 0) throw new IllegalStateException("amount must me greater than zero");
    }

    public Transfer asOutgoing() {
        return new Transfer(fromAccount, fromAccount, toAccount, amount.negate(), now(systemUTC()));
    }

    public Transfer asIncoming() {
        return new Transfer(toAccount, fromAccount, toAccount, amount, now(systemUTC()));
    }
}
