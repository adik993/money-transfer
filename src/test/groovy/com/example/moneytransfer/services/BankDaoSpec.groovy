package com.example.moneytransfer.services

import com.example.moneytransfer.model.Account
import com.example.moneytransfer.model.Transfer
import org.sql2o.Connection
import org.sql2o.Query
import org.sql2o.Sql2o
import spock.lang.Specification

import static java.time.Clock.systemUTC
import static java.time.LocalDateTime.now

class BankDaoSpec extends Specification {
    def sql2o = Mock(Sql2o)
    def underSpec = new BankDao(sql2o)

    def "createAccount - inserts new account with specified balance and returns account with it's id"() {
        given:
        def balance = 100.0
        def con = Mock(Connection)
        def query = Mock(Query)

        when:
        def result = underSpec.createAccount(balance)

        then:
        1 * sql2o.open() >> con
        1 * con.createQuery({ it.contains("insert into account") } as String) >> query
        1 * query.addParameter("balance", balance) >> query
        1 * query.executeUpdate() >> con
        1 * con.getKey(Long) >> 1L
        1 * con.commit()
        result.balance == balance
        result.id == 1
    }

    def "fetchAccountById - returns account by id"() {
        given:
        def id = 1
        def con = Mock(Connection)
        def query = Mock(Query)
        def account = new Account(1, 100.0)

        when:
        def result = underSpec.fetchAccountById(id)

        then:
        1 * sql2o.open() >> con
        1 * con.createQuery({ it.contains("from account") } as String) >> query
        1 * query.addParameter("id", id) >> query
        1 * query.executeAndFetchFirst(Account) >> account
        result.is(account)
    }

    def "fetchTransfersByAccountId - returns transfers by account id"() {
        given:
        def id = 1
        def con = Mock(Connection)
        def query = Mock(Query)
        def transfers = [new Transfer(1, 1, 2, -10.0, null)]

        when:
        def result = underSpec.fetchTransfersByAccountId(id)

        then:
        1 * sql2o.open() >> con
        1 * con.createQuery({ it.contains("from transfer") } as String) >> query
        1 * query.addParameter("id", id) >> query
        1 * query.executeAndFetch(Transfer) >> transfers
        result.is(transfers)
    }

    def "persist - inserts transfer"() {
        given:
        def con = Mock(Connection)
        def query = Mock(Query)
        def now = now(systemUTC())
        def transfer = new Transfer(1, 1, 2, -10.0, now)

        when:
        underSpec.persist(transfer)

        then:
        1 * sql2o.beginTransaction() >> con
        1 * con.createQuery({ it.contains("insert into transfer") } as String) >> query
        1 * query.bind(transfer) >> query
        1 * query.executeUpdate()
        1 * con.commit()
    }

    def "lockAccounts - begins transactions and selects accounts for update"() {
        given:
        def ids = [1L, 2L]
        def account1 = new Account(1, 100.0)
        def account2 = new Account(2, 100.0)
        def tx = Mock(Connection)
        def query = Mock(Query)

        when:
        def result = underSpec.lockAccounts(ids)

        then:
        1 * sql2o.beginTransaction() >> tx
        1 * tx.createQuery("select * from account where id in (:ids) for update") >> query
        1 * query.addParameter("ids", ids) >> query
        1 * query.executeAndFetch(Account) >> [account1, account2]
        result.getAccount(1) == account1
        result.getAccount(2) == account2
    }

    def "lockAccounts - rollbacks when unable to retrieve accounts"() {
        given:
        def ids = [1, 2]
        def tx = Mock(Connection)
        def query = Mock(Query)

        when:
        underSpec.lockAccounts(ids)

        then:
        thrown(RuntimeException)
        1 * sql2o.beginTransaction() >> tx
        1 * tx.createQuery("select * from account where id in (:ids) for update") >> query
        1 * query.addParameter("ids", ids) >> query
        1 * query.executeAndFetch(Account) >> { throw new RuntimeException("bang") }
        1 * tx.rollback()
    }

    def "lockAccounts - throws IllegalStateException when any of the accounts is not found in db"() {
        given:
        def ids = [1, 2]
        def account1 = new Account(1, 100.0)
        def tx = Mock(Connection)
        def query = Mock(Query)

        when:
        underSpec.lockAccounts(ids)

        then:
        thrown(IllegalStateException)
        1 * sql2o.beginTransaction() >> tx
        1 * tx.createQuery("select * from account where id in (:ids) for update") >> query
        1 * query.addParameter("ids", ids) >> query
        1 * query.executeAndFetch(Account) >> [account1]
        1 * tx.rollback()
    }

    def "AccountLock - close closes transaction"() {
        given:
        def tx = Mock(Connection)
        def lock = new BankDao.AccountsLock(tx, [:])

        when:
        lock.close()

        then:
        1 * tx.close()
    }

    def "AccountLock - commit commits transaction"() {
        given:
        def tx = Mock(Connection)
        def lock = new BankDao.AccountsLock(tx, [:])

        when:
        lock.commit()

        then:
        1 * tx.commit()
    }

    def "AccountLock - getAccount returns account by id"() {
        given:
        def tx = Mock(Connection)
        def account = new Account(1, 100.0)
        def lock = new BankDao.AccountsLock(tx, [(account.id): account])

        when:
        def resultExisting = lock.getAccount(1)
        def resultNonExisting = lock.getAccount(2)

        then:
        resultExisting == account
        resultNonExisting == null
    }
}
