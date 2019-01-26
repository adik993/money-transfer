package com.example.moneytransfer.controllers;

import com.example.moneytransfer.dto.CreateAccountRequest;
import com.example.moneytransfer.dto.TransferRequest;
import com.example.moneytransfer.model.Account;
import com.example.moneytransfer.model.Transfer;
import com.example.moneytransfer.services.BankService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import spark.Request;
import spark.Response;

import java.util.List;

import static com.example.moneytransfer.App.gson;

@RequiredArgsConstructor
public class BankController {
    private final BankService service;

    public Account createAccount(Request request, Response response) {
        val dto = gson.fromJson(request.body(), CreateAccountRequest.class);
        dto.validate();
        return service.createAccount(dto);
    }

    public Account getAccount(Request request, Response response) {
        val id = Long.parseLong(request.params("id"));
        return service.getAccount(id);
    }

    public List<Transfer> transfers(Request request, Response response) {
        val id = Long.parseLong(request.params("id"));
        return service.transfers(id);
    }

    public Transfer transfer(Request request, Response response) {
        val dto = gson.fromJson(request.body(), TransferRequest.class);
        dto.validate();
        return service.transfer(dto);
    }
}
