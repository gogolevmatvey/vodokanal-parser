package org.example.parser.model.dto;

import java.util.List;

/**
 * Полная информация о лицевом счете для отображения в UI
 */
public class FullAccountDTO {
    private final Long accountId;
    private final String accountNumber;
    private final String payerName;
    private final String fullAddress;
    private final List<BillingPeriodDTO> periods;
    
    public FullAccountDTO(Long accountId, String accountNumber, String payerName, 
                          String fullAddress, List<BillingPeriodDTO> periods) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.payerName = payerName;
        this.fullAddress = fullAddress;
        this.periods = periods;
    }
    
    public Long getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getPayerName() { return payerName; }
    public String getFullAddress() { return fullAddress; }
    public List<BillingPeriodDTO> getPeriods() { return periods; }
}
