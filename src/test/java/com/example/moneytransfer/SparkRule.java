package com.example.moneytransfer;

import groovyx.net.http.RESTClient;
import groovyx.net.http.URIBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.net.URISyntaxException;

import static spark.Spark.*;

public class SparkRule implements TestRule {
    private final Runnable appRunner;
    private final String host = "localhost";
    private String url;

    public SparkRule(Runnable appRunner) {
        this.appRunner = appRunner;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                port(0);
                ipAddress(host);
                appRunner.run();
                awaitInitialization();
                url = new URIBuilder("").setHost(host).setPort(port()).setScheme("http").toString();
                base.evaluate();
                stop();
                awaitStop();
            }
        };
    }

    public RESTClient newClient() throws URISyntaxException {
        return new RESTClient(url);
    }
}
