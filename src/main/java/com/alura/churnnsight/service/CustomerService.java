package com.alura.churnnsight.service;

import com.alura.churnnsight.dto.consult.*;
import com.alura.churnnsight.exception.NotFoundException;
import com.alura.churnnsight.model.Customer;
import com.alura.churnnsight.repository.CustomerRepository;
import com.alura.churnnsight.repository.PredictionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    public DataCustomerDetail getCustomer(String customerId){
        
        var customer = customerRepository.findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new NotFoundException("Customer no encontrado: " + customerId));
        return new DataCustomerDetail(customer);
        
    }

    public Page<DataCustomerDetail> getCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(DataCustomerDetail::new);
    }


    public Page<DataProductDetail> getCustomerProducts(String customerId, Pageable pageable) {
        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new NotFoundException("Customer no encontrado: " + customerId));
        return customerRepository.findProductsByCustomerId(customer.getId(), pageable).map(DataProductDetail::new);
    }

    public DataCustomerStatusDetail getCustomerStatus(String customerId) {
        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new NotFoundException("Customer no encontrado: " + customerId));
        var customerStatus = customerRepository.findStatusByCustomerId(customer.getId());
        return new DataCustomerStatusDetail(customerStatus);
    }

    public Page<DataAccountDetail> getCustomerAccounts(String customerId, Pageable pageable) {
        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new NotFoundException("Customer no encontrado: " + customerId));
        return customerRepository.findAccountsByCustomerId(customer.getId(), pageable).map(DataAccountDetail::new);
    }
}
