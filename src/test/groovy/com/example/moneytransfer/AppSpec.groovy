package com.example.moneytransfer

import com.example.moneytransfer.controllers.BankController
import com.example.moneytransfer.dto.CreateAccountRequest
import com.example.moneytransfer.dto.TransferRequest
import com.example.moneytransfer.exceptions.InsufficientFundsException
import com.example.moneytransfer.model.Account
import com.example.moneytransfer.model.Transfer
import org.junit.Rule
import spark.Request
import spark.Response
import spock.lang.Specification
import spock.lang.Unroll

import static groovyx.net.http.ContentType.JSON
import static java.time.Clock.systemUTC
import static java.time.LocalDateTime.now
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

class AppSpec extends Specification {
    def controller = Mock(BankController)
    def underSpec = new App(controller)

    @Rule
    SparkRule sparkRule = new SparkRule({ underSpec.init() })

    def "POST /accounts - creates account and returns dto"() {
        given:
        def dto = new CreateAccountRequest(1000.0)

        when:
        def result = sparkRule.newClient().post(path: "/accounts", body: dto, requestContentType: JSON)

        then:
        1 * controller.createAccount(_ as Request, _ as Response) >> new Account(1, 1000.0)
        result.data.id == 1
        result.data.balance == 1000
    }

    def "GET /accounts/:id - returns specific account information"() {
        given:
        def id = 1

        when:
        def result = sparkRule.newClient().get(path: "/accounts/${id}")

        then:
        1 * controller.getAccount(_ as Request, _ as Response) >> new Account(1, 5.0)
        result.data.id == 1
        result.data.balance == 5
    }

    def "GET /accounts/:id/transfers - returns account transfers"() {
        given:
        def id = 1
        def now = now(systemUTC())

        when:
        def result = sparkRule.newClient().get(path: "/accounts/${id}/transfers")

        then:
        1 * controller.transfers(_ as Request, _ as Response) >> [new Transfer(id, id, 2, 10.0, now)]
        result.data.size() == 1
        result.data[0].fromAccount == 1
        result.data[0].toAccount == 2
        result.data[0].amount == 10
        result.data[0].timestamp == ISO_LOCAL_DATE_TIME.format(now)
    }

    def "POST /transfers - transfers money"() {
        given:
        def request = new TransferRequest(1, 2, 10.0)
        def now = now(systemUTC())
        def transfer = new Transfer(1, 1, 2, 10.0, now)

        when:
        def result = sparkRule.newClient().post(path: '/transfers', body: request, requestContentType: JSON)

        then:
        1 * controller.transfer(_ as Request, _ as Response) >> transfer
        result.data.fromAccount == 1
        result.data.toAccount == 2
        result.data.amount == 10
        result.data.timestamp == ISO_LOCAL_DATE_TIME.format(now)
    }

    @Unroll
    def "on exception - #exception respond with #status http status and error details in body"() {
        given:
        def request = new TransferRequest(1, 2, 10.0)

        when:
        def client = sparkRule.newClient()
        client.handler.failure = client.handler.success
        def result = client.post(path: '/transfers', body: request, requestContentType: JSON)

        then:
        1 * controller.transfer(_ as Request, _ as Response) >> { throw exception }
        result.status == status
        result.data.exception == exception.getClass().getName()
        result.data.message == exception.getMessage()

        where:
        exception                                            | status
        new InsufficientFundsException("insufficient funds") | 400
        new IllegalStateException("account missing")         | 400
        new RuntimeException("unknown")                      | 500

    }
}
