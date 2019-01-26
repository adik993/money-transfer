package com.example.moneytransfer.services;

import com.example.moneytransfer.model.Account;
import com.example.moneytransfer.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.io.Closeable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class BankDao {
    private final Sql2o sql2o;

    Account createAccount(BigDecimal balance) {
        try (val con = sql2o.open()) {
            Long key = con.createQuery("insert into account(balance) values(:balance)")
                    .addParameter("balance", balance)
                    .executeUpdate().getKey(Long.class);
            con.commit();
            return new Account(key, balance);
        }
    }

    Account fetchAccountById(long id) {
        try (val con = sql2o.open()) {
            return con.createQuery("select * from account where id = :id")
                    .addParameter("id", id)
                    .executeAndFetchFirst(Account.class);
        }
    }

    List<Transfer> fetchTransfersByAccountId(long id) {
        try (val con = sql2o.open()) {
            return con.createQuery("select aggregate_id as aggregateId, from_account as fromAccount, " +
                    "to_account as toAccount, amount, timestamp " +
                    "from transfer where aggregate_id = :id order by id")
                    .addParameter("id", id)
                    .executeAndFetch(Transfer.class);
        }
    }

    AccountsLock lockAccounts(List<Long> accountIds) {
        val transaction = sql2o.beginTransaction();
        try {
            val accounts = transaction.createQuery("select * from account where id in (:ids) for update")
                    .addParameter("ids", accountIds)
                    .executeAndFetch(Account.class).stream()
                    .collect(toMap(Account::getId, identity()));
            if (accounts.size() != accountIds.size())
                throw new IllegalStateException("one of the accounts does not exist");
            return new AccountsLock(transaction, accounts);
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    void persist(Transfer transfer) {
        try (val con = sql2o.beginTransaction()) {
            con.createQuery("insert into transfer(aggregate_id, from_account, to_account, amount, timestamp) " +
                    "values(:aggregateId, :fromAccount, :toAccount, :amount, :timestamp)")
                    .bind(transfer)
                    .executeUpdate();
            con.commit();
        }
    }

    @RequiredArgsConstructor
    static class AccountsLock implements AutoCloseable, Closeable {
        private final Connection transaction;
        private final Map<Long, Account> lockedAccounts;

        public Account getAccount(long id) {
            return lockedAccounts.get(id);
        }

        public void commit() {
            transaction.commit();
        }

        public void close() {
            transaction.close();
        }
    }
}
