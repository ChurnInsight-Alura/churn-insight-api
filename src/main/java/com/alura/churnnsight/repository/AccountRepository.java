package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account,Long> {
}
