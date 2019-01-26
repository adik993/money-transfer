package com.example.moneytransfer.services

import com.example.moneytransfer.dto.CreateAccountRequest
import com.example.moneytransfer.dto.TransferRequest
import com.example.moneytransfer.exceptions.InsufficientFundsException
import com.example.moneytransfer.model.Account
import com.example.moneytransfer.model.Transfer
import org.sql2o.Connection
import spock.lang.Specification

import static java.time.Clock.systemUTC
import static java.time.LocalDateTime.now

class BankServiceSpec extends Specification {
    def dao = Mock(BankDao)
    def underSpec = new BankService(dao)

    def "create account"() {
        given:
        def request = new CreateAccountRequest(1000.0)
        def account = new Account(1, 1000.0)

        when:
        def result = underSpec.createAccount(request)

        then:
        1 * dao.createAccount(request.balance) >> account
        result.is(account)
    }

    def "get account by id"() {
        given:
        def account = new Account(id: 1, balance: 5.0)
        def transfers = [new Transfer(1, 2, 1, 10.0, null),
                         new Transfer(1, 1, 2, -3.0, null)]

        when:
        def result = underSpec.getAccount(1)

        then:
        1 * dao.fetchAccountById(1) >> account
        1 * dao.fetchTransfersByAccountId(1) >> transfers
        result.id == 1
        result.balance == 12.0
    }

    def "get account transfers"() {
        given:
        def id = 1
        def now = now(systemUTC())
        def transfers = [new Transfer(id, id, 2, -10.0, now),
                         new Transfer(id, 2, id, 5.0, now)]

        when:
        def result = underSpec.transfers(id)

        then:
        1 * dao.fetchTransfersByAccountId(id) >> transfers
        result.is(transfers)
    }

    def "transfer - persists incoming and outgoing transfer in one transaction with pessimistic lock on the accounts"() {
        given:
        def request = new TransferRequest(1, 2, 10.0)
        def transfers = [new Transfer(1, 1, 3, 5.0, null)]
        def tx = Mock(Connection)
        def lock = new BankDao.AccountsLock(tx, [1L: new Account(id: 1, balance: 20),
                                                 2L: new Account(id: 2, balance: 0)])

        when:
        underSpec.transfer(request)

        then:
        1 * dao.lockAccounts([1, 2]) >> lock
        1 * dao.fetchTransfersByAccountId(1) >> transfers
        1 * dao.persist({ equalTransfer(it as Transfer, request.asOutgoing()) } as Transfer)
        1 * dao.persist({ equalTransfer(it as Transfer, request.asIncoming()) } as Transfer)
        1 * tx.commit()
    }

    def "transfer - throws InsufficientFundsException and rollbacks transaction when from account has insufficient funds"() {
        given:
        def request = new TransferRequest(1, 2, 10.0)
        def transfers = [new Transfer(1, 1, 3, -5.0, null)]
        def tx = Mock(Connection)
        def lock = new BankDao.AccountsLock(tx, [1L: new Account(id: 1, balance: 10),
                                                 2L: new Account(id: 2, balance: 0)])

        when:
        underSpec.transfer(request)

        then:
        thrown(InsufficientFundsException)
        1 * dao.lockAccounts([1, 2]) >> lock
        1 * dao.fetchTransfersByAccountId(1) >> transfers
        1 * tx.close()
        0 * _
    }

    static equalTransfer(Transfer actual, Transfer expected) {
        return actual.aggregateId == expected.aggregateId &&
                actual.fromAccount == expected.fromAccount &&
                actual.toAccount == expected.toAccount &&
                actual.amount == expected.amount
    }
}
