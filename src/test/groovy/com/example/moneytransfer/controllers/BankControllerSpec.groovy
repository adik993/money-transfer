package com.example.moneytransfer.controllers

import com.example.moneytransfer.dto.CreateAccountRequest
import com.example.moneytransfer.dto.TransferRequest
import com.example.moneytransfer.model.Account
import com.example.moneytransfer.model.Transfer
import com.example.moneytransfer.services.BankService
import spark.Request
import spark.Response
import spock.lang.Specification

import static groovy.json.JsonOutput.toJson
import static java.time.Clock.systemUTC
import static java.time.LocalDateTime.now

class BankControllerSpec extends Specification {
    def service = Mock(BankService)
    def underSpec = new BankController(service)

    def "createAccount - call the service and return created account"() {
        given:
        def body = toJson(new CreateAccountRequest(1000.0))
        def request = Mock(Request)
        def account = new Account(1, 1000.0)

        when:
        def result = underSpec.createAccount(request, null)

        then:
        1 * request.body() >> body
        1 * service.createAccount({ it.balance == 1000 } as CreateAccountRequest) >> account
        result.id == account.id
        result.balance == account.balance
    }

    def "createAccount - validates request"() {
        given:
        def body = toJson(new CreateAccountRequest(-1.0))
        def request = Mock(Request)

        when:
        underSpec.createAccount(request, null)

        then:
        thrown(IllegalStateException)
        1 * request.body() >> body
        0 * _
    }

    def "getAccount - returns account by id"() {
        given:
        def id = 1
        def request = Mock(Request)
        def account = new Account(1, 500.0)

        when:
        def result = underSpec.getAccount(request, null)

        then:
        1 * request.params("id") >> id.toString()
        1 * service.getAccount(id) >> account
        result.id == account.id
        result.balance == account.balance
    }

    def "transfers - returns list of transfers for account"() {
        given:
        def id = 1
        def now = now(systemUTC())
        def request = Mock(Request)
        def transfer = new Transfer(id, id, 2, -10.0, now)

        when:
        def result = underSpec.transfers(request, null)

        then:
        1 * request.params("id") >> id.toString()
        1 * service.transfers(id) >> [transfer]
        result == [transfer]
    }

    def "transfer - calls service with parsed dto"() {
        given:
        def body = toJson(new TransferRequest(1, 2, 10.0))
        def now = now(systemUTC())
        def transfer = new Transfer(1, 1, 2, -10.0, now)
        def request = Mock(Request)
        def response = Mock(Response)

        when:
        def result = underSpec.transfer(request, response)

        then:
        1 * request.body() >> body
        1 * service.transfer({
            it.fromAccount == 1 && it.toAccount == 2 && it.amount == 10
        } as TransferRequest) >> transfer
        result == transfer
    }

    def "transfer - validates request"() {
        given:
        def body = toJson(new TransferRequest(1, 2, -10.0))
        def request = Mock(Request)
        def response = Mock(Response)

        when:
        underSpec.transfer(request, response)

        then:
        thrown(IllegalStateException)
        1 * request.body() >> body
        0 * _
    }
}
