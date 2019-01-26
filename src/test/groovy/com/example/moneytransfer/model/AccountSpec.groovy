package com.example.moneytransfer.model

import com.example.moneytransfer.exceptions.InsufficientFundsException
import spock.lang.Specification

class AccountSpec extends Specification {

    def "applyTransfer - adds amount to the account when account has sufficient funds"() {
        given:
        def underSpec = new Account(id: 1, balance: 10.0)
        def transfer = new Transfer(1, 1, 2, amount, null)

        when:
        underSpec.applyTransfer(transfer)

        then:
        underSpec.balance == 10 + amount

        where:
        amount | _
        -4.0   | _
        -10.0  | _
        5.0    | _
    }

    def "applyTransfer - throws InsufficientFundsException when from account has insufficient funds"() {
        given:
        def underSpec = new Account(id: 1, balance: 2.0)
        def transfer = new Transfer(1, 1, 2, -4.0, null)

        when:
        underSpec.applyTransfer(transfer)

        then:
        thrown(InsufficientFundsException)
        underSpec.balance == 2
    }

    def "applyTransfers - applies all transfers"() {
        given:
        def underSpec = new Account(id: 1, balance: 10.0)
        def transfers = [new Transfer(1, 1, 2, -4.0, null),
                         new Transfer(1, 2, 1, 2.0, null)]

        when:
        underSpec.applyTransfers(transfers)

        then:
        underSpec.balance == 8.0
    }
}
