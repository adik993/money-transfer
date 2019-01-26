package com.example.moneytransfer.dto


import spock.lang.Specification

class TransferRequestSpec extends Specification {
    def "validate - accepts amount greater than zero"() {
        given:
        def underSpec = new TransferRequest(1, 2, 1.0)

        when:
        underSpec.validate()

        then:
        noExceptionThrown()
    }

    def "validate - throws IllegalStateException when amount is 0 or below"() {
        given:
        def underSpec = new TransferRequest(1, 2, amount)

        when:
        underSpec.validate()

        then:
        thrown(IllegalStateException)

        where:
        amount | _
        0.0    | _
        -1.0   | _
    }

    def "getAccountIds - return from and to account ids as list"() {
        given:
        def underSpec = new TransferRequest(1, 2, 1.0)

        when:
        def result = underSpec.getAccountIds()

        then:
        result == [1L, 2L]
    }

    def "asOutgoing - create outgoing transfer"() {
        def underSpec = new TransferRequest(1, 2, 10.0)

        when:
        def result = underSpec.asOutgoing()

        then:
        result.aggregateId == 1
        result.fromAccount == 1
        result.toAccount == 2
        result.amount == -10.0
        result.timestamp != null
    }

    def "asIncoming - create outgoing transfer"() {
        def underSpec = new TransferRequest(1, 2, 10.0)

        when:
        def result = underSpec.asIncoming()

        then:
        result.aggregateId == 2
        result.fromAccount == 1
        result.toAccount == 2
        result.amount == 10.0
        result.timestamp != null
    }
}
