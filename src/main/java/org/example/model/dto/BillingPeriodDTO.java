package org.example.model.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для периода начисления
 */
public class BillingPeriodDTO {
    private final Long id;
    private final String period;
    private final BigDecimal totalAmount;
    private final List<MeterChargeDTO> meterCharges;
    
    public BillingPeriodDTO(Long id, String period, BigDecimal totalAmount, List<MeterChargeDTO> meterCharges) {
        this.id = id;
        this.period = period;
        this.totalAmount = totalAmount;
        this.meterCharges = meterCharges;
    }
    
    public Long getId() { return id; }
    public String getPeriod() { return period; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<MeterChargeDTO> getMeterCharges() { return meterCharges; }
}
