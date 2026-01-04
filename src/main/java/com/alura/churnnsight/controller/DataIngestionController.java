package com.alura.churnnsight.controller;

import com.alura.churnnsight.dto.creation.*;
import com.alura.churnnsight.service.DataIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ingestion")
public class DataIngestionController {
    @Autowired
    private DataIngestionService dataIngestionService;

    @Transactional
    @PostMapping("/customer")
    public ResponseEntity<Map<String,String>> createCustomer(@RequestBody DataCreateCustomer dataCreateCustomer){
        dataIngestionService.createCustomer(dataCreateCustomer);
        return genericResponse();
    }
    @Transactional
    @PostMapping("/account")
    public ResponseEntity<Map<String,String>> createAccount(@RequestBody DataCreateAccount dataCreateAccount){
        dataIngestionService.createAccount(dataCreateAccount);
        return genericResponse();
    }
    @Transactional
    @PostMapping("/customer-status")
    public ResponseEntity<Map<String,String>> createCustomerStatus(@RequestBody DataCreateCustomerStatus dataCreateCustomerStatus){
        dataIngestionService.createCustomerStatus(dataCreateCustomerStatus);
        return genericResponse();
    }
    @Transactional
    @PostMapping("/product")
    public ResponseEntity<Map<String,String>> createProduct(@RequestBody DataCreateProduct dataCreateProduct){
        dataIngestionService.createProduct(dataCreateProduct);
        return genericResponse();
    }
    @Transactional
    @PostMapping("/assign-product")
    public ResponseEntity<Map<String,String>> assignProduct(@RequestBody DataAssignProduct dataAssignProduct){
        dataIngestionService.assignProduct(dataAssignProduct);
        return genericResponse();
    }


    public ResponseEntity<Map<String,String>> genericResponse(){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("status","Created"));
    }
}
