package org.example.parser.model.dto;

/**
 * DTO для отображения лицевого счета в UI
 */
public class AccountDTO {
    private final Long id;
    private final String accountNumber;
    private final String payerName;
    
    public AccountDTO(Long id, String accountNumber, String payerName) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.payerName = payerName;
    }
    
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getPayerName() { return payerName; }
    
    @Override
    public String toString() {
        return accountNumber + " - " + payerName;
    }
}
