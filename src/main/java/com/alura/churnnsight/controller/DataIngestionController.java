package com.alura.churnnsight.controller;

import com.alura.churnnsight.dto.creation.*;
import com.alura.churnnsight.service.DataIngestionService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Map<String,String>> createCustomer(@Valid @RequestBody DataCreateCustomer dataCreateCustomer){
        dataIngestionService.createCustomer(dataCreateCustomer);
        return genericResponse();
    }
    @Transactional
    @PostMapping("/account")
    public ResponseEntity<Map<String,String>> createAccount(@Valid @RequestBody DataCreateAccount dataCreateAccount){
        dataIngestionService.createAccount(dataCreateAccount);
        return genericResponse();
    }
    @Transactional
    @PostMapping("/customer-status")
    public ResponseEntity<Map<String,String>> createCustomerStatus(@Valid @RequestBody DataCreateCustomerStatus dataCreateCustomerStatus){
        dataIngestionService.createCustomerStatus(dataCreateCustomerStatus);
        return genericResponse();
    }
    @Transactional
    @PostMapping("/product")
    public ResponseEntity<Map<String,String>> createProduct(@Valid @RequestBody DataCreateProduct dataCreateProduct){
        dataIngestionService.createProduct(dataCreateProduct);
        return genericResponse();
    }
    @Transactional
    @PostMapping("/assign-product")
    public ResponseEntity<Map<String,String>> assignProduct(@Valid @RequestBody DataAssignProduct dataAssignProduct){
        dataIngestionService.assignProduct(dataAssignProduct);
        return genericResponse();
    }

    @PostMapping("/customer-transaction")
    public ResponseEntity<Map<String,String>> createTx(@Valid @RequestBody DataCreateCustomerTransaction dto) {
        dataIngestionService.createCustomerTransaction(dto);
        return genericResponse();
    }

    @PostMapping("/customer-session")
    public ResponseEntity<Map<String,String>> createSession(@Valid @RequestBody DataCreateCustomerSession dto) {
        dataIngestionService.createCustomerSession(dto);
        return genericResponse();
    }

    public ResponseEntity<Map<String,String>> genericResponse(){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "status","Created",
                        "message", "Registro creado correctamente"));
    }

}
