package org.example.parser.model;

import java.util.Objects;

/**
 * Лицевой счет
 */
public class Account {
    private Long id;
    private Long apartmentId;
    private String accountNumber;
    private String payerName;
    
    public Account() {}
    
    public Account(Long id, Long apartmentId, String accountNumber, String payerName) {
        this.id = id;
        this.apartmentId = apartmentId;
        this.accountNumber = accountNumber;
        this.payerName = payerName;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getApartmentId() { return apartmentId; }
    public void setApartmentId(Long apartmentId) { this.apartmentId = apartmentId; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Account{id=" + id + ", accountNumber='" + accountNumber + "', payerName='" + payerName + "'}";
    }
}
