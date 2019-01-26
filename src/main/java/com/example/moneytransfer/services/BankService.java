package com.example.moneytransfer.services;

import com.example.moneytransfer.dto.CreateAccountRequest;
import com.example.moneytransfer.dto.TransferRequest;
import com.example.moneytransfer.exceptions.InsufficientFundsException;
import com.example.moneytransfer.model.Account;
import com.example.moneytransfer.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.List;

@RequiredArgsConstructor
public class BankService {
    private final BankDao dao;

    public Account createAccount(CreateAccountRequest request) {
        return dao.createAccount(request.getBalance());
    }

    public Account getAccount(long id) {
        val account = dao.fetchAccountById(id);
        replayTransfers(account);
        return account;
    }

    public List<Transfer> transfers(long id) {
        return dao.fetchTransfersByAccountId(id);
    }

    public Transfer transfer(TransferRequest request) throws InsufficientFundsException {
        try (val lock = dao.lockAccounts(request.getAccountIds())) {
            Account from = lock.getAccount(request.getFromAccount());
            replayTransfers(from);
            val outgoing = request.asOutgoing();
            from.applyTransfer(outgoing);
            dao.persist(outgoing);
            dao.persist(request.asIncoming());
            lock.commit();
            return outgoing;
        }
    }

    private void replayTransfers(Account account) {
        val transfers = dao.fetchTransfersByAccountId(account.getId());
        account.applyTransfers(transfers);
    }
}
