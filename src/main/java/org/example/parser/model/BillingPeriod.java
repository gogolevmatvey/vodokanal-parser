package org.example.parser.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Период начисления
 */
public class BillingPeriod {
    private Long id;
    private Long accountId;
    private String period;
    private BigDecimal totalAmount;
    
    public BillingPeriod() {}
    
    public BillingPeriod(Long id, Long accountId, String period, BigDecimal totalAmount) {
        this.id = id;
        this.accountId = accountId;
        this.period = period;
        this.totalAmount = totalAmount;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BillingPeriod that = (BillingPeriod) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "BillingPeriod{id=" + id + ", period='" + period + "'}";
    }
}
