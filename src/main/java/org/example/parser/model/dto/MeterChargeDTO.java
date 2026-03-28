package org.example.parser.model.dto;

import java.math.BigDecimal;

/**
 * DTO для начисления по прибору учета
 */
public class MeterChargeDTO {
    private final Long id;
    private final String meterName;
    private final BigDecimal reading;
    private final BigDecimal amount;
    
    public MeterChargeDTO(Long id, String meterName, BigDecimal reading, BigDecimal amount) {
        this.id = id;
        this.meterName = meterName;
        this.reading = reading;
        this.amount = amount;
    }
    
    public Long getId() { return id; }
    public String getMeterName() { return meterName; }
    public BigDecimal getReading() { return reading; }
    public BigDecimal getAmount() { return amount; }
}
