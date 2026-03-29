package org.example.model.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.example.model.common.Address;
import org.example.model.domain.MeterCharge;

/**
 * Распаршенная запись
 */
public class ParsedRecord {
    private final String accountNumber;
    private final String payerName;
    private final Address address;
    private final String billingPeriod;
    private final BigDecimal totalAmount;
    private final List<MeterCharge> meterCharges;
    
    public ParsedRecord(String accountNumber, String payerName, Address address,
                        String billingPeriod, BigDecimal totalAmount, List<MeterCharge> meterCharges) {
        this.accountNumber = accountNumber;
        this.payerName = payerName;
        this.address = address;
        this.billingPeriod = billingPeriod;
        this.totalAmount = totalAmount;
        this.meterCharges = meterCharges != null ? meterCharges : new ArrayList<>();
    }
    
    public String getAccountNumber() { return accountNumber; }
    public String getPayerName() { return payerName; }
    public Address getAddress() { return address; }
    public String getBillingPeriod() { return billingPeriod; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<MeterCharge> getMeterCharges() { return meterCharges; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedRecord that = (ParsedRecord) o;
        return Objects.equals(accountNumber, that.accountNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }
    
    @Override
    public String toString() {
        return "ParsedRecord{accountNumber='" + accountNumber + "', period='" + billingPeriod + "'}";
    }
}
