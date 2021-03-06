/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.example.moneytransfer;

import com.example.moneytransfer.controllers.BankController;
import com.example.moneytransfer.db.LocalDateTimeConverter;
import com.example.moneytransfer.exceptions.InsufficientFundsException;
import com.example.moneytransfer.services.BankDao;
import com.example.moneytransfer.services.BankService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.quirks.NoQuirks;
import spark.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;

import static com.fatboyindustrial.gsonjavatime.Converters.registerLocalDateTime;
import static spark.Spark.*;

@Slf4j
@RequiredArgsConstructor
public class App {
    public static final Gson gson = registerLocalDateTime(new GsonBuilder()).create();
    private final BankController bankController;

    void init() {
        post("/accounts", bankController::createAccount, gson::toJson);
        get("/accounts/:id", bankController::getAccount, gson::toJson);
        get("/accounts/:id/transfers", bankController::transfers, gson::toJson);
        post("/transfers", bankController::transfer, gson::toJson);
        before((request, response) -> log.info("{} {}", request.requestMethod(), request.url()));
        after((request, response) -> response.type("application/json"));
        exception(IllegalStateException.class, errorHandler(400));
        exception(InsufficientFundsException.class, errorHandler(400));
        exception(Exception.class, errorHandler(500));
    }

    private ExceptionHandler<? super Exception> errorHandler(int status) {
        return (exception, request, response) -> {
            response.type("application/json");
            response.status(status);
            response.body(exceptionToJson(exception));
        };
    }

    private String exceptionToJson(Exception exception) {
        val fields = new HashMap<String, String>();
        fields.put("exception", exception.getClass().getName());
        fields.put("message", exception.getMessage());
        return gson.toJson(fields);
    }

    public static void main(String[] args) {
        val map = new HashMap<Class, Converter>();
        map.put(LocalDateTime.class, new LocalDateTimeConverter());
        val sql2o = new Sql2o("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "", new NoQuirks(map));
        initDb(sql2o);
        val dao = new BankDao(sql2o);
        val bankService = new BankService(dao);
        val bankController = new BankController(bankService);
        new App(bankController).init();
    }

    private static void initDb(Sql2o sql2o) {
        try (val transaction = sql2o.beginTransaction()) {
            transaction.createQuery("drop all objects delete files").executeUpdate();
            transaction.createQuery("create table account(" +
                    "id bigint auto_increment primary key," +
                    "balance decimal(19, 4))")
                    .executeUpdate();
            transaction.createQuery("create table transfer(" +
                    "id bigint auto_increment primary key," +
                    "aggregate_id bigint not null," +
                    "from_account bigint not null," +
                    "to_account bigint not null," +
                    "amount decimal(19, 4)," +
                    "timestamp timestamp," +
                    "foreign key (from_account) references account(id)," +
                    "foreign key (to_account) references account(id))")
                    .executeUpdate();
            transaction.commit();
        }
    }
}
