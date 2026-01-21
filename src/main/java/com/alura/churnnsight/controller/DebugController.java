package com.alura.churnnsight.controller;

import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.consult.DataAccountDetail;
import com.alura.churnnsight.dto.consult.DataCustomerDetail;
import com.alura.churnnsight.dto.consult.DataCustomerStatusDetail;
import com.alura.churnnsight.dto.consult.DataProductDetail;
import com.alura.churnnsight.repository.AccountRepository;
import com.alura.churnnsight.repository.CustomerRepository;
import com.alura.churnnsight.repository.CustomerStatusRepository;
import com.alura.churnnsight.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CustomerStatusRepository customerStatusRepository;

    @GetMapping("/customers")
    public ResponseEntity<Page<DataCustomerDetail>> getAllCustomers(@PageableDefault(size = 20) Pageable pageable) {
        var page = customerRepository.findAll(pageable).map(DataCustomerDetail::new);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/accounts")
    public ResponseEntity<Page<DataAccountDetail>> getAllAccounts(@PageableDefault(size = 20) Pageable pageable) {
        var page = accountRepository.findAll(pageable).map(DataAccountDetail::new);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/products")
    public ResponseEntity<Page<DataProductDetail>> getAllProducts(@PageableDefault(size = 20) Pageable pageable) {
        var page = productRepository.findAll(pageable).map(DataProductDetail::new);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/customer-statuses")
    public ResponseEntity<Page<DataCustomerStatusDetail>> getAllCustomerStatus(@PageableDefault(size = 20) Pageable pageable) {
        var page = customerStatusRepository.findAll(pageable).map(DataCustomerStatusDetail::new);
        return ResponseEntity.ok(page);
    }
    @GetMapping("/features/{customerId}")
    public ResponseEntity<DataMakePrediction> getCustomerFeatures(@PathVariable String customerId) {
        var customer = customerRepository.findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        Long id = customer.getId();

        int isActiveMember =
                customer.getStatus() != null &&
                        Boolean.TRUE.equals(customer.getStatus().getIsActiveMember())
                        ? 1
                        : 0;

        DataMakePrediction data = new DataMakePrediction(
                customer,
                customer.getTenure(refDate),
                customerRepository.CountBalanceByCostumerId(id),
                customerRepository.CountProductsByCostumerId(id),
                isActiveMember
        );
        return ResponseEntity.ok(data);
    }

}
