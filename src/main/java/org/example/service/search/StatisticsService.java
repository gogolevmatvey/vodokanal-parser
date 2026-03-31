package org.example.service.search;

import org.example.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для получения статистики базы данных
 */
@Service
public class StatisticsService {

    private final LocalityRepository localityRepo;
    private final StreetRepository streetRepo;
    private final HouseRepository houseRepo;
    private final ApartmentRepository apartmentRepo;
    private final AccountRepository accountRepo;
    private final BillingPeriodRepository billingPeriodRepo;

    public StatisticsService(LocalityRepository localityRepo,
                             StreetRepository streetRepo,
                             HouseRepository houseRepo,
                             ApartmentRepository apartmentRepo,
                             AccountRepository accountRepo,
                             BillingPeriodRepository billingPeriodRepo) {
        this.localityRepo = localityRepo;
        this.streetRepo = streetRepo;
        this.houseRepo = houseRepo;
        this.apartmentRepo = apartmentRepo;
        this.accountRepo = accountRepo;
        this.billingPeriodRepo = billingPeriodRepo;
    }

    /**
     * Получить статистику базы данных
     */
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("localities", (long) localityRepo.findAll().size());
        stats.put("streets", (long) streetRepo.findAll().size());
        stats.put("houses", (long) houseRepo.findAll().size());
        stats.put("apartments", (long) apartmentRepo.findAll().size());
        stats.put("accounts", (long) accountRepo.findAll().size());
        stats.put("billingPeriods", (long) billingPeriodRepo.findAll().size());
        return stats;
    }
}
