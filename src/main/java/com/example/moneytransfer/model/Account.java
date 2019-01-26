package com.example.moneytransfer.model;

import com.example.moneytransfer.exceptions.InsufficientFundsException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;

import java.math.BigDecimal;
import java.util.List;

import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static lombok.AccessLevel.PRIVATE;

@Getter
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor
public class Account {
    private Long id;
    private BigDecimal balance = ZERO;

    public void applyTransfer(Transfer transfer) throws InsufficientFundsException {
        val amount = transfer.getAmount();
        if (ZERO.compareTo(balance.add(amount)) > 0)
            throw new InsufficientFundsException(format("Account %d has insufficient funds to debit %.2f", id, amount));
        balance = balance.add(amount);
    }

    public void applyTransfers(List<Transfer> transfers) throws InsufficientFundsException {
        transfers.forEach(this::applyTransfer);
    }
}
