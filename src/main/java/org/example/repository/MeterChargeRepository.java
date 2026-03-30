package org.example.repository;

import org.example.model.domain.MeterCharge;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с начислениями по приборам учета
 */
public interface MeterChargeRepository extends BaseRepository<MeterCharge, Long> {

    /**
     * Найти начисления по идентификатору лицевого счета и периоду
     */
    List<MeterCharge> findByAccountIdAndPeriod(Long accountId, String period);

    /**
     * Найти начисление по имени прибора, счету и периоду
     */
    Optional<MeterCharge> findByAccountIdAndPeriodAndMeterName(Long accountId, String period, String meterName);
}
