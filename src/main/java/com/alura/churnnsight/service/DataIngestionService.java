package com.alura.churnnsight.service;

import com.alura.churnnsight.dto.creation.*;
import com.alura.churnnsight.exception.CreationException;
import com.alura.churnnsight.exception.NotFoundException;
import com.alura.churnnsight.model.*;
import com.alura.churnnsight.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;

    @Autowired
    private CustomerSessionRepository customerSessionRepository;


    public void createCustomer(DataCreateCustomer dataCreateCustomer){
        var customer = new Customer(dataCreateCustomer);
        if (customerRepository.findByCustomerIdIgnoreCase(dataCreateCustomer.customerId()).isPresent()) {
            throw new CreationException("Customer ya existe: " + dataCreateCustomer.customerId());
        }
        customerRepository.save(customer);
    }
    public void createAccount(DataCreateAccount dataCreateAccount){
        var customer = customerRepository.findByCustomerIdIgnoreCase(dataCreateAccount.customerId())
                .orElseThrow(()->
                        new NotFoundException("Cliente no encontrado: " + dataCreateAccount.customerId())
                );
        var account = new Account(null,customer,dataCreateAccount.balance(),null,null);
        accountRepository.save(account);
    }
    public void createCustomerStatus(DataCreateCustomerStatus dataCreateCustomerStatus){
        var customer = customerRepository.findByCustomerIdIgnoreCase(dataCreateCustomerStatus.customerId())
                .orElseThrow(()->
                        new NotFoundException("Cliente no encontrado: " + dataCreateCustomerStatus.customerId())
                );
        boolean hasCrCardBool = dataCreateCustomerStatus.hasCrCard() != null
                && dataCreateCustomerStatus.hasCrCard() == 1;

        var customerStatus = new CustomerStatus(
                null,
                customer,
                dataCreateCustomerStatus.creditScore(),
                dataCreateCustomerStatus.isActiveMember(),
                hasCrCardBool
        );
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
                        new NotFoundException("Producto no encontrado: " + dataAssignProduct.productName())
                );

        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(dataAssignProduct.customerId())
                .orElseThrow(() ->
                        new NotFoundException("Customer no encontrado: " + dataAssignProduct.customerId())
                );

        customer.addProduct(product);
        customerRepository.save(customer);
    }

    public void createCustomerTransaction(DataCreateCustomerTransaction dto) {
        Customer customer = customerRepository.findByCustomerIdIgnoreCase(dto.customerId())
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + dto.customerId()));

        if (customerTransactionRepository
                .findByCustomerIdAndTransactionId(customer.getId(), dto.transactionId())
                .isPresent()) {
            throw new CreationException("Transaction ya existe: " + dto.transactionId());
        }

        CustomerTransaction tx = new CustomerTransaction();
        tx.setTransactionId(dto.transactionId());
        tx.setCustomer(customer);
        tx.setTransactionDate(dto.transactionDate());
        tx.setAmount(dto.amount());
        tx.setTransactionType(dto.transactionType());

        customerTransactionRepository.save(tx);
    }

    public void createCustomerSession(DataCreateCustomerSession dto) {
        Customer customer = customerRepository.findByCustomerIdIgnoreCase(dto.customerId())
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + dto.customerId()));

        if (customerSessionRepository
                .findByCustomerIdAndSessionId(customer.getId(), dto.sessionId())
                .isPresent()) {
            throw new CreationException("Session ya existe: " + dto.sessionId());
        }

        CustomerSession s = new CustomerSession();
        s.setSessionId(dto.sessionId());
        s.setCustomer(customer);
        s.setSessionDate(dto.sessionDate());
        s.setDurationMin(dto.durationMin());
        s.setUsedTransfer(dto.usedTransfer());
        s.setUsedPayment(dto.usedPayment());
        s.setUsedInvest(dto.usedInvest());
        s.setOpenedPush(dto.openedPush());
        s.setFailedLogin(dto.failedLogin());

        customerSessionRepository.save(s);
    }


}
