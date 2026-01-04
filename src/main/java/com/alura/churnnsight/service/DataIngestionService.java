package com.alura.churnnsight.service;

import com.alura.churnnsight.dto.creation.*;
import com.alura.churnnsight.exception.CreationException;
import com.alura.churnnsight.model.Account;
import com.alura.churnnsight.model.Customer;
import com.alura.churnnsight.model.CustomerStatus;
import com.alura.churnnsight.model.Product;
import com.alura.churnnsight.repository.AccountRepository;
import com.alura.churnnsight.repository.CustomerRepository;
import com.alura.churnnsight.repository.CustomerStatusRepository;
import com.alura.churnnsight.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataIngestionService {
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    CustomerStatusRepository customerStatusRepository;
    @Autowired
    ProductRepository productRepository;


    public void createCustomer(DataCreateCustomer dataCreateCustomer){
        var customer = new Customer(dataCreateCustomer);
        customerRepository.save(customer);
    }
    public void createAccount(DataCreateAccount dataCreateAccount){
        var customer = customerRepository.findByCustomerIdIgnoreCase(dataCreateAccount.customerId())
                .orElseThrow(()->
                        new CreationException("Cliente no encontrado: " + dataCreateAccount.customerId())
                );
        var account = new Account(null,customer,dataCreateAccount.balance(),null,null);
        accountRepository.save(account);
    }
    public void createCustomerStatus(DataCreateCustomerStatus dataCreateCustomerStatus){
        var customer = customerRepository.findByCustomerIdIgnoreCase(dataCreateCustomerStatus.customerId())
                .orElseThrow(()->
                        new CreationException("Cliente no encontrado: " + dataCreateCustomerStatus.customerId())
                );
        var customerStatus = new CustomerStatus(null,customer,dataCreateCustomerStatus.creditScore(),dataCreateCustomerStatus.isActiveMember());
        customerStatusRepository.save(customerStatus);
    }
    public void createProduct(DataCreateProduct dataCreateProduct){
        var product = new Product(null,dataCreateProduct.name());
        productRepository.save(product);
    }
    public void assignProduct(DataAssignProduct dataAssignProduct){
        Product product = productRepository
                .findByNameIgnoreCase(dataAssignProduct.productName())
                .orElseThrow(() ->
                        new CreationException("Producto no encontrado: " + dataAssignProduct.productName())
                );

        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(dataAssignProduct.customerId())
                .orElseThrow(() ->
                        new CreationException("Customer no encontrado: " + dataAssignProduct.customerId())
                );

        customer.addProduct(product);
        customerRepository.save(customer);
    }

}
