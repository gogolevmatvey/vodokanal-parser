package org.example.controller.api;

import org.example.dto.response.ApiResponse;
import org.example.model.domain.Account;
import org.example.model.dto.AccountDTO;
import org.example.service.search.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API для работы с лицевыми счетами
 */
@RestController
@RequestMapping("/api/apartments/{apartmentId}/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Получить лицевые счета по квартире
     * GET /api/apartments/{apartmentId}/accounts
     */
    @GetMapping
    public ApiResponse<List<AccountDTO>> getAccounts(@PathVariable Long apartmentId) {
        List<Account> accounts = accountService.findByApartmentId(apartmentId);
        List<AccountDTO> response = accounts.stream()
            .map(a -> new AccountDTO(a.getId(), a.getAccountNumber(), a.getPayerName()))
            .toList();
        
        return ApiResponse.success(response);
    }

    /**
     * Поиск по номеру лицевого счета
     * GET /api/accounts/search?number=900045955
     */
    @GetMapping("/search")
    public ApiResponse<AccountDTO> searchByAccountNumber(@RequestParam String number) {
        return accountService.findByAccountNumber(number)
            .map(a -> ApiResponse.success(new AccountDTO(a.getId(), a.getAccountNumber(), a.getPayerName())))
            .orElse(ApiResponse.error("ACCOUNT_NOT_FOUND", "Лицевой счет не найден"));
    }
}
