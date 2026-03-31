package org.example.service.search;

import org.example.model.domain.Account;
import org.example.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с лицевыми счетами
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Найти лицевые счета по квартире
     */
    public List<Account> findByApartmentId(Long apartmentId) {
        return accountRepository.findByApartmentId(apartmentId);
    }

    /**
     * Найти лицевой счет по номеру
     */
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Найти лицевой счет по ID
     */
    public Account findById(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Лицевой счет не найден: " + id));
    }
}
