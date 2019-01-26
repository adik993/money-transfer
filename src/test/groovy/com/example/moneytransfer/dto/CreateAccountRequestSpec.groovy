package com.example.moneytransfer.dto

import spock.lang.Specification

class CreateAccountRequestSpec extends Specification {
    def "validate - accepts balance 0 and greater"() {
        given:
        def underSpec = new CreateAccountRequest(balance)

        when:
        underSpec.validate()

        then:
        noExceptionThrown()

        where:
        balance | _
        0.0     | _
        10.1    | _
    }

    def "validate - throws IllegalStateException when balance is below 0"() {
        given:
        def underSpec = new CreateAccountRequest(-1.0)

        when:
        underSpec.validate()

        then:
        thrown(IllegalStateException)
    }
}
