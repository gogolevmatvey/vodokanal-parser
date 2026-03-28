package org.example.parser.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Начисление по прибору учета (для БД)
 */
public class MeterCharge {
    private Long id;
    private Long billingPeriodId;
    private String meterName;
    private BigDecimal reading;
    private BigDecimal amount;
    
    public MeterCharge() {}
    
    public MeterCharge(Long id, Long billingPeriodId, String meterName, BigDecimal reading, BigDecimal amount) {
        this.id = id;
        this.billingPeriodId = billingPeriodId;
        this.meterName = meterName;
        this.reading = reading;
        this.amount = amount;
    }
    
    public MeterCharge(String meterName, BigDecimal reading, BigDecimal amount) {
        this.meterName = meterName;
        this.reading = reading;
        this.amount = amount;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getBillingPeriodId() { return billingPeriodId; }
    public void setBillingPeriodId(Long billingPeriodId) { this.billingPeriodId = billingPeriodId; }
    
    public String getMeterName() { return meterName; }
    public void setMeterName(String meterName) { this.meterName = meterName; }
    
    public BigDecimal getReading() { return reading; }
    public void setReading(BigDecimal reading) { this.reading = reading; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeterCharge that = (MeterCharge) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "MeterCharge{id=" + id + ", meterName='" + meterName + "'}";
    }
}
