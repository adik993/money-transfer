package com.example.moneytransfer

import com.example.moneytransfer.dto.CreateAccountRequest
import com.example.moneytransfer.dto.TransferRequest
import com.example.moneytransfer.model.Transfer
import org.junit.Rule
import spock.lang.Specification

import java.time.LocalDateTime

import static groovyx.net.http.ContentType.JSON
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

class AppIT extends Specification {
    @Rule
    SparkRule sparkRule = new SparkRule({ App.main() })

    def "transfering money debits from account, credits to account and stores transfer in db"() {
        given:
        def fromId = createAccount(100.0)
        def toId = createAccount(10.0)
        def otherId = createAccount(999.0)
        def request = new TransferRequest(fromId, toId, 25.5)

        when:
        sparkRule.newClient().post(path: "/transfers", body: request, requestContentType: JSON)

        then:
        getBalance(fromId) == 74.5
        getBalance(toId) == 35.5
        getBalance(otherId) == 999
        assertTransaction getTransfers(fromId), [new Transfer(fromId, fromId, toId, -25.5, null)]
        assertTransaction getTransfers(toId), [new Transfer(toId, fromId, toId, 25.5, null)]
        getTransfers(otherId) == []
    }

    def "transfering money fails when from account has insufficient funds and app state is unchanged"() {
        given:
        def fromId = createAccount(100.0)
        def toId = createAccount(10.0)
        def request = new TransferRequest(fromId, toId, 100.01)

        when:
        def client = sparkRule.newClient()
        client.handler.failure = client.handler.success
        def result = client.post(path: "/transfers", body: request, requestContentType: JSON)

        then:
        result.data.exception == 'com.example.moneytransfer.exceptions.InsufficientFundsException'
        result.data.message == "Account ${fromId} has insufficient funds to debit -100.01"
        getBalance(fromId) == 100
        getBalance(toId) == 10
        getTransfers(fromId) == []
        getTransfers(toId) == []
    }

    def "app is in consistent state even with concurrent access"() {
        given:
        def nTransfersPerThread = 100
        def nThreadsPerAccount = 2
        def first = createAccount(500.0)
        def second = createAccount(500.0)
        def debitFirstAccountRequest = new TransferRequest(first, second, 2.0)
        def debitSecondAccountRequest = new TransferRequest(second, first, 1.0)
        Closure debitFirstAccount = createTransferClojure(nTransfersPerThread, debitFirstAccountRequest)
        Closure debitSecondAccount = createTransferClojure(nTransfersPerThread, debitSecondAccountRequest)

        when:
        def debitFirstAccountThreads = (1..nThreadsPerAccount).collect { Thread.start(debitFirstAccount) }
        def debitSecondAccountThreads = (1..nThreadsPerAccount).collect { Thread.start(debitSecondAccount) }
        debitFirstAccountThreads.forEach { it.join() }
        debitSecondAccountThreads.forEach { it.join() }

        then: 'total balance in the system is still the same(no a single penny was lost)'
        getBalance(first) == 300.0 // 500 - 400 + 200 = 300
        getBalance(second) == 700.0 // 500 - 200 + 400 = 700
        and: 'there were twice the amount of transfers registered for each account(outgoing + incoming)'
        getTransfers(first).size() == nTransfersPerThread * nThreadsPerAccount * 2
        getTransfers(second).size() == nTransfersPerThread * nThreadsPerAccount * 2
    }

    private Closure createTransferClojure(int nTransfers, TransferRequest request) {
        return {
            nTransfers.times {
                sparkRule.newClient().post(path: "/transfers", body: request, requestContentType: JSON)
            }
        }
    }

    long createAccount(BigDecimal balance) {
        def dto = new CreateAccountRequest(balance)
        def result = sparkRule.newClient().post(path: "/accounts", body: dto, requestContentType: JSON)
        return result.data.id
    }

    BigDecimal getBalance(long id) {
        def result = sparkRule.newClient().get(path: "/accounts/${id}")
        return result.data.balance
    }

    List<Transfer> getTransfers(long id) {
        def result = sparkRule.newClient().get(path: "/accounts/${id}/transfers")
        return result.data.collect {
            //noinspection GroovyAssignabilityCheck
            new Transfer(it.aggregateId, it.fromAccount, it.toAccount, it.amount,
                    ISO_LOCAL_DATE_TIME.parse(it.timestamp, LocalDateTime.&from))
        }
    }

    private static void assertTransaction(List<Transfer> actuals, List<Transfer> expects) {
        [actuals, expects].transpose().forEach { Transfer actual, Transfer expected ->
            assert actual.fromAccount == expected.fromAccount
            assert actual.toAccount == expected.toAccount
            assert actual.amount == expected.amount
            assert actual.timestamp != null
        }
    }
}
