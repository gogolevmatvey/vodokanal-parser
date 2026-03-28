package org.example.repository;

import org.example.parser.model.BillingPeriod;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с периодами начислений
 */
public interface BillingPeriodRepository extends BaseRepository<BillingPeriod, Long> {
    
    /**
     * Найти период по идентификатору счета и периоду
     */
    Optional<BillingPeriod> findByAccountIdAndPeriod(Long accountId, String period);
    
    /**
     * Найти все периоды по идентификатору счета
     */
    List<BillingPeriod> findByAccountId(Long accountId);
}
