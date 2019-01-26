# Money Transfer Backend

Simple implementation of money transfer API.

 * It uses in memory H2 DB
 * To ensure data consistency pessimistic lock is acquired on the accounts being involved in the money transfer
 * Event sourcing approach is used to store the account balances 

## Libraries used

 * spark
 * gson
 * sql2o
 * h2
 
## How to run

Directly from gradle:
```
./gradlew run
```
or produce fat jar and run it:
```
./gradlew fatJar && java -jar ./build/libs/money-transfer-fat.jar
```

## API

 * `POST /accounts` new account  
    Body: `{"balance":  10.0}`  
    Response: `{"id": 1, "balance": 10.0}`
 * `GET /accounts/:id` account balance  
    Response: `{"id": 1, "balance": 10.0}`
 * `GET /accounts/:id/transfers` account transfer history  
    `[{"fromAccount": 1, "toAccount": 2, "amount": 100, "timestamp": "2019-01-27T21:30:36.023"}]`
 * `POST /transfers` new transfer  
    Body: `{"fromAccount": 1, "toAccount": 2, "amount": 100.0}`  
    Response: `{"fromAccount": 1, "toAccount": 2, "amount": 100, "timestamp": "2019-01-27T21:30:36.023"}`

Error responses format:
 ```json
{
    "exception": "com.example.moneytransfer.exceptions.InsufficientFundsException",
    "message": "Account 1 has insufficient funds to debit 100.00"
}
```