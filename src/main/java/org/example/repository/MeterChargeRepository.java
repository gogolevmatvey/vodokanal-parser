package org.example.repository;

import org.example.parser.model.MeterCharge;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с начислениями по приборам учета
 */
public interface MeterChargeRepository extends BaseRepository<MeterCharge, Long> {
    
    /**
     * Найти начисления по идентификатору периода
     */
    List<MeterCharge> findByBillingPeriodId(Long billingPeriodId);
    
    /**
     * Найти начисление по имени прибора и периоду
     */
    Optional<MeterCharge> findByBillingPeriodIdAndMeterName(Long billingPeriodId, String meterName);
}
