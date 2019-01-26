package com.example.moneytransfer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Transfer {
    private long aggregateId;
    private long fromAccount;
    private long toAccount;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
